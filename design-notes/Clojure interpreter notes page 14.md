- To try to understand how squint does lazy seqs, I compiled a tiny program:
- ```clojure
  (defn fib
    ([]
     (fib 1 1))
    ([a b]
     (lazy-seq (cons a (fib b (+ a b))))))
  
  (prn (take 5 (fib)))
  ```
- The output (once I formatted it) turns out to be rather enlightening, not so much for lazy seqs but for function arities:
- ```js
  var fib = (function () {
      let f1 = (function (var_args) {
          let G__45 = arguments["length"];
          switch (G__45) {
              case 0:
                  return f1.cljs$core$IFn$_invoke$arity$0();
                  break;
              case 2:
                  return f1.cljs$core$IFn$_invoke$arity$2((arguments[0]), (arguments[1]));
                  break;
              default:
                  throw new Error(squint_core.str("Invalid arity: ", squint_core.alength(arguments)))
          }
      });
      f1["cljs$core$IFn$_invoke$arity$0"] = (function () {
          return fib(1, 1);
      });
      f1["cljs$core$IFn$_invoke$arity$2"] = (function (a, b) {
          return new squint_core.LazySeq((function () {
              return squint_core.cons(a, fib(b, (a + b)));
          }));
      });
      f1["cljs$lang$maxFixedArity"] = 2;
      return f1;
  })();
  ```
- If the way I did arities turns out to have problems, this might be a better approach. The function defines another function which has a switch statement that dispatches on `arguments["length"]`, with each arity returning another function and calling it. Those functions are then defined as properties on the outer function. Pretty cool! That way all the logic is contained within the function itself, there need be no other logic when calling it. I remember thinking of something like that when I was brainstorming, but it's very illuminating to see it all right here. I wonder what else I can learn from squint
- I want to watch those videos again by fogus and mike fikes explaining clojurescript internals, those are amazing
- ClojureScript Anatomy Michael Fogus https://www.youtube.com/watch?v=lC39ifspIf4
- Mike Fikes explains the ClojureScript Compiler (from https://cljs.pro): https://www.youtube.com/watch?v=kBKIGj1_WAo
- Now that I understand squint's output, I wonder if I could reverse engineer lazy seqs.
- I might as well drop it in there, because even if it doesn't work, it won't hurt anything... just don't use it!
- Fogus' talk just explained the `maxFixedArity` thing... it's so you can pass an infinite seq to a function and it only realizes that many. The logic is handled by `applyTo`, which is attached to functions that have varargs. He has a slide that shows the code for Clojurescript's `apply`:
- ![image.png](../assets/image_1693715581412_0.png)
- Fogus wrote extensively on his blog, omg this is amazing: http://blog.fogus.me/tag/clj-compilation/
- It's like the macroexpansion of the talk.
- phronmophobic responded to my question and pointed me back to the source, which has the bits that I was missing:
- ```clojure
  (defn ^{:private true}
    maybe-destructured
    [params body]
    (if (every? symbol? params)
      (cons params body)
      (loop [params params
             new-params (with-meta [] (meta params))
             lets []]
        (if params
          (if (symbol? (first params))
            (recur (next params) (conj new-params (first params)) lets)
            (let [gparam (gensym "p__")]
              (recur (next params) (conj new-params gparam)
                     (-> lets (conj (first params)) (conj gparam)))))
          `(~new-params
            (let ~lets
              ~@body))))))
  
  ;redefine fn with destructuring and pre/post conditions
  (defmacro fn
    "params => positional-params*, or positional-params* & rest-param
    positional-param => binding-form
    rest-param => binding-form
    binding-form => name, or destructuring-form
  
    Defines a function.
  
    See https://clojure.org/reference/special_forms#fn for more information"
    {:added "1.0", :special-form true,
     :forms '[(fn name? [params* ] exprs*) (fn name? ([params* ] exprs*)+)]}
    [& sigs]
      (let [name (if (symbol? (first sigs)) (first sigs) nil)
            sigs (if name (next sigs) sigs)
            sigs (if (vector? (first sigs)) 
                   (list sigs) 
                   (if (seq? (first sigs))
                     sigs
                     ;; Assume single arity syntax
                     (throw (IllegalArgumentException. 
                              (if (seq sigs)
                                (str "Parameter declaration " 
                                     (first sigs)
                                     " should be a vector")
                                (str "Parameter declaration missing"))))))
            psig (fn* [sig]
                   ;; Ensure correct type before destructuring sig
                   (when (not (seq? sig))
                     (throw (IllegalArgumentException.
                              (str "Invalid signature " sig
                                   " should be a list"))))
                   (let [[params & body] sig
                         _ (when (not (vector? params))
                             (throw (IllegalArgumentException. 
                                      (if (seq? (first sigs))
                                        (str "Parameter declaration " params
                                             " should be a vector")
                                        (str "Invalid signature " sig
                                             " should be a list")))))
                         conds (when (and (next body) (map? (first body))) 
                                             (first body))
                         body (if conds (next body) body)
                         conds (or conds (meta params))
                         pre (:pre conds)
                         post (:post conds)                       
                         body (if post
                                `((let [~'% ~(if (< 1 (count body)) 
                                              `(do ~@body) 
                                              (first body))]
                                   ~@(map (fn* [c] `(assert ~c)) post)
                                   ~'%))
                                body)
                         body (if pre
                                (concat (map (fn* [c] `(assert ~c)) pre) 
                                        body)
                                body)]
                     (maybe-destructured params body)))
            new-sigs (map psig sigs)]
        (with-meta
          (if name
            (list* 'fn* name new-sigs)
            (cons 'fn* new-sigs))
          (meta &form))))
  ```
- I'm glad he pointed that out, I should have looked before asking the question, lol
- # Lazy seqs attempt number 14
	- So here is the output of a lazy fib in squint, where I took the class from:
	- ```js
	  f1["cljs$core$IFn$_invoke$arity$2"] = (function (a, b) {
	          return new squint_core.LazySeq((function () {
	              return squint_core.cons(a, fib(b, (a + b)));
	          }));
	  ```
	- I have the thing hooked up and while it's not working, I'm not able to determine why. A problem is that there are 2 layers of lazy things which complicates things. Here is the LazySeq class:
	- ```js
	  export class LazySeq {
	      constructor(f) {
	          this.name = 'LazySeq'
	          this.f = f;
	      }
	      *[Symbol.iterator]() {
	          yield* this.f();
	      }
	  }
	  ```
	- I need to understand what this is. I hate all this iterable bs. I hate protocols.
	- Wtf is `*[Symbol.iterator]`? Wtf is the star?
	- > The function* declaration creates a binding of a new generator function to a given name
	- > A function* declaration creates a GeneratorFunction object. Each time when a generator function is called, it returns a new Generator object, which conforms to the iterator protocol. When the iterator's next() method is called, the generator function's body is executed until the first yield expression, which specifies the value to be returned from the iterator or, with yield*, delegates to another generator function.
	- ### Generator as a computed property
	- ```js
	  class Foo {
	    *[Symbol.iterator]() {
	      yield 1;
	      yield 2;
	    }
	  }
	  
	  const SomeObj = {
	    *[Symbol.iterator]() {
	      yield "a";
	      yield "b";
	    },
	  };
	  
	  console.log(Array.from(new Foo())); // [ 1, 2 ]
	  console.log(Array.from(SomeObj)); // [ 'a', 'b' ]
	  ```
	- That's it.
	- I've noticed something strange when I print the body passed to the lazy-seq macro:
	- ```clojure
	  ((cons a (fib b (+ a b))))
	  ```
	- Could that be a clue? it has an extra set of parentheses
	- ...which is correct, because it is the sequence representing the & body. oh, well. I thought I found a problem, but the macroexpansion of lazy-seq is correctly
	- ```clojure
	  (new LazySeq (fn [] (cons a (fib b (+ a b)))))
	  ```
- This is wack. I think I'd rather work on destructuring, that's far more useful. This lazy bs will be here later. It's just... it seems like it's so close... Switching gears...
- ## map destructuring
  collapsed:: true
	- Tracing an associative binding `[{:keys [w b]} {:w [2 4] :b [6 6]}]`
	- It's this bit of code that runs while building the map of transforms
	- ```clojure
	  (assoc transforms mk (fn [k] (keyword (or mkns (namespace k)) (name k))))
	  ```
	- It's associng a function into the map which will take
	- `b` = `{:keys [w b]}`
	- `(keys b)` = `(:keys)`
	- `transforms` = `{}`
	- Hold on... let's back up. I'm working on this chunk of code
	- ```clojure
	  (let* [transforms
	         (reduce
	          (fn [transforms mk]
	            (if (keyword? mk)
	              (let* [mkns (namespace mk)
	                     mkn (name mk)]
	                    (cond (= mkn "keys")
	                          (assoc transforms
	                                 mk
	                                 (fn [k] (keyword (or mkns (namespace k)) (name k))))
	                          (= mkn "syms")
	                          (assoc transforms
	                                 mk
	                                 #(list
	                                   `quote
	                                   (symbol
	                                    (or mkns
	                                        (namespace %)) (name %))))
	                          (= mkn "strs")
	                          (assoc transforms mk str)
	                          :else transforms))
	              transforms))
	          {}
	          (keys b))]
	        (reduce
	         (fn [bes entry]
	           (reduce (fn [a b] (assoc a b ((val entry) b)))
	                   (dissoc bes (key entry))
	                   ((key entry) bes)))
	         (dissoc b :as :or)
	         transforms))
	  ```
	- And specifically this part
	- ```clojure
	  (reduce
	          (fn [transforms mk]
	            (if (keyword? mk)
	              (let* [mkns (namespace mk)
	                     mkn (name mk)]
	                    (cond (= mkn "keys")
	                          (assoc transforms
	                                 mk
	                                 (fn [k] (keyword (or mkns (namespace k)) (name k))))
	                          (= mkn "syms")
	                          (assoc transforms
	                                 mk
	                                 #(list
	                                   `quote
	                                   (symbol
	                                    (or mkns
	                                        (namespace %)) (name %))))
	                          (= mkn "strs")
	                          (assoc transforms mk str)
	                          :else transforms))
	              transforms))
	          {}
	          (keys b))
	  ```
	- `b` = `{:keys [w b]}`
	- `(keys b)` = `(:keys)`
	- So we're reducing over `(:keys)`
	- The first (and only) one is `:keys`
	- `(keyword? mk)` evaluates to true, so we run
	- ```clojure
	  (let* [mkns (namespace mk)
	         mkn (name mk)]
	                    (cond (= mkn "keys")
	                          (assoc transforms
	                                 mk
	                                 (fn [k] (keyword (or mkns (namespace k)) (name k))))
	                          (= mkn "syms")
	                          (assoc transforms
	                                 mk
	                                 #(list
	                                   `quote
	                                   (symbol
	                                    (or mkns
	                                        (namespace %)) (name %))))
	                          (= mkn "strs")
	                          (assoc transforms mk str)
	                          :else transforms))
	  ```
	- `mkns` is `nil`
	- `mkn` is `"keys"`
	- so we run
	- ```clojure
	  (assoc transforms mk (fn [k] (keyword (or mkns (namespace k)) (name k))))
	  ```
	- ok we're back here. I guess I'm just trying to understand what we're doing.
	- The result is a map: `{:keys #function[fn]}`
	- ok, I guess that's fine.
	- So the `k` in the lambda function is one of the values in the vector, and we're turning them into keywords, got it.
	- Oh, and I figured out the problem! it's because `keyword` needs to accept 1 or 2 args, for an optional ns.
	- So far so good... the value `transforms` is bound to is `[map__2 {:w [2 4] :b [6 6]} map__2 map__2]`
	- So now we're up to this `reduce`:
	- ```clojure
	  (reduce
	   (fn [bes entry]
	     (reduce (fn [a b] (assoc a b ((val entry) b)))
	  
	             (dissoc bes (key entry))
	             ((key entry) bes)))
	   (dissoc b :as :or)
	   transforms)
	  ```
	- `b` is `{:keys [w b]}`
	- The `init` value, `(dissoc b :as :or)` is `{:keys [w b]}`
	- This is a double reduce so it's a bit confusing. We enter the outer one with proper values:
	- `bes: {:keys [w b]} entry: (:keys #function[fn])`
	- Then we step into the inner one:
	- ```clojure
	  (reduce (fn [a b] (assoc a b ((val entry) b)))
	          (dissoc bes (key entry))
	          ((key entry) bes))
	  ```
	- `(dissoc bes (key entry))` = `{}`
	- `((key entry) bes)`, the seq being reduced over, is what fails. I think. It may just fail to print because remember this is all running inside a macro, and what fails is trying to print `w`
	- I think I should start with one that doesn't use :keys.
	- ```clojure
	  (def client {:name "Super Co."
	               :location "Philadelphia"
	               :description "The worldwide leader in plastic tableware."})
	    
	  (destructure '[{name :name
	                  location :location
	                  description :description} client])
	  ```
	- This is failing on contains_Q because it's trying to find something in null
	- omg it works
	- ```clojure
	  => [map__2 client map__2 map__2 
	      name (get map__2 :name)
	      location (get map__2 :location)
	      description (get map__2 :description)]
	  ```
	- cool!
	- ```clojure
	  (let [{name :name
	         location :location
	         description :description} client]
	    [name location description])
	  => ["Super Co." "Philadelphia" "The worldwide leader in plastic tableware."]
	  ```
- I think I've been negligent about cloning shit and now I've got mutation happening
- `take` fails on sets:
- ```clojure
  (take 1 #{"cot" "hot" "bat" "fat"})
  => 
  Error: coll.slice is not a function
  ```
- maps too. I'm surprised I didn't notice that yet. same for drop... all it needs to do is call seq on the coll
- This is failing, but I swear I fixed this the other day
- `(partition 2 2 [0] '(9 5))`
- ## `recur` without loop
- ```clojure
  (defn myfn [s res]
    (if (empty? s) res
      (recur (rest s) (conj res (first s)))))
  
  (myfn (range 10) [])
  => [0 1 2 3 4 5 6 7 8 9] 
  
  (defn myfn
    ([s] (myfn s []))
    ([s res]
    (if (empty? s) res
      (recur (rest s) (conj res (first s))))))
  
  (myfn (range 10))
  => [0 1 2 3 4 5 6 7 8 9] 
  ```
- For some reason, this is failing and it wasn't before:
- ```clojure
  (apply str (interpose " " []))
  => 
  Error: Cannot read properties of null (reading 'slice') 
  ```
- I wonder what changed?
- Even just `(interpose " " [])`.
- interpose uses interleave. Which works:
- ```clojure
  (interleave (repeat (count []) " ") [])
  => [] 
  ```
- So it must be `drop`. That's the only other thing
- Yep, fixed it. I remember now, I recently added seq to drop so it would work on maps and sets. mystery solved.
- 148 tests passed 😍 most I've had so far
- I need to make it actually evaluate map keys, because that was disabled in mal for some reason and I hadn't questioned it until now. But because of that, it will do this:
- ```clojure
  {'name "Super Co."}
  => {(quote name) "Super Co."} 
  ```
- It works right if you use `hash-map`:
- ```clojure
  (hash-map (symbol "name") "Super Co.")
  => {name "Super Co."} 
  ```
- Craig Andera asked a good question, whether it will destructure maps with strings or symbol keys. It already works on strings, but symbols are still failing even though `get` works, I'll have to fix that
- ## Lazy seqs... my way
	- So how in the world is this supposed to work
	- ```clojure
	  (defn fib
	    ([]
	     (fib 1 1))
	    ([a b]
	     (lazy-seq (cons a (fib b (+ a b))))))
	  ```
	- Like, how would you even execute it in your head?
	- The first call `(fib)` calls the second arity with `(fib 1 1)`, so `a` and `b` are defined as 2 `1`sj.
	- ```clojure
	  (fib 1 1)
	  (def a 1)
	  (def b 1)
	  ```
	- The body is wrapped in a thunk:
	- ```clojure
	  (fn []
	    (cons a (fib b (+ a b))))
	  ```
	- So when that is called, it somehow needs to `(cons 1 '(1 (+ 1 1)))`.
	- I'm missing some basic information. Let's look at [the Clojure source](https://github.com/clojure/clojure/blob/master/src/jvm/clojure/lang/LazySeq.java).
	- I read somewhere that it's actually pretty straightforward.
- [[Clojure interpreter notes page 15]]