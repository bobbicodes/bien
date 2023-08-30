- ## More `lazy-seq` fun
	- So there will be no modifications to the function type, and no notion of a "lazy function. The `lazySeq` will be an object containing a function which is the body wrapped in a thunk so it can be called when we want.
- ## Function arities
	- Solving this problem might help unfold lazy seqs, because I'm going to have to dig into and change what a function is.   So... what's a function?
	- ### Functions
	- A fn is created like this
	- `types._function(EVAL, Env, body, env, args);`
	- It's weird how we have to pass `EVAL` and `Env` to it, like could it not access it anyway?
	- An anonymous function only has one arity.
	- So I suppose we don't make any difference to what a function is. It's fine.
- So in light of this... since we are passing the evaluator to the function... We can pass a different one! Let's make another function called `eval_lazy_seq`, which will be the eval passed to `_function` so that when it is called, it will not evaluate in an endless loop like the regular one. Rather, it will loop until either the requested elements have been realized, or the call to the body returns `nil`.
- Let's contrive a simple example that even I can understand.
- lol, there's an "even simpler example" in Joy of Clojure:
- ```clojure
  (defn simple-range [i limit]
    (lazy-seq
     (when (< i limit)
       (cons i (simple-range (inc i) limit)))))
  ```
- What if the evaluator just contains another parameter that is an alternate condition for the `while` loop? Otherwise, the function will be nearly identical.
- So the definition will look like this:
- ```js
  function _EVAL(ast, env, loopCondition) {
    //console.log("Calling _EVAL", ast)
    while (loopCondition || true) {
  ```
- So we won't be passing a different eval, we'll just call eval with a loop condition.
- Maybe it could work like macros? they're functions that are "special"
- So let's look at it from the calling side. `take` is a good one to start with. It needs to specially handle lazy seqs.
- Here is a basic test example so I can start playing with it.
- ```clojure
  (take 5 (lazy-seq (range 100)))
  ```
- hmm, it seems that anonymous functions have been broken for idk how long.
- Well... since this is a teaching project, I decided to make a tutorial. Let's just call it [[Lisp tutorial]]
- I disabled the multiarity stuff because I want to try something different. If nothing else, it will help me understand the way it works better.
- You could rewrite a multiarity function into one that dispatches on arg length. So we could just make a function that does that.
- So if we can rewrite
- ```clojure
  (defn a
    ([] "no args")
    ([n] "one arg")
    ([n & more] "variadic"))
  ```
- to be
- ```clojure
  (defn a
    )
  ```
- Maybe I'll make `defn` a proper macro. Here's the Clojure source for defn
- ```clojure
  (def 
  
   ^{:doc "Same as (def name (fn [params* ] exprs*)) or (def
      name (fn ([params* ] exprs*)+)) with any doc-string or attrs added
      to the var metadata. prepost-map defines a map with optional keys
      :pre and :post that contain collections of pre or post conditions."
     :arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                  [name doc-string? attr-map? ([params*] prepost-map? body)+ attr-map?])
     :added "1.0"}
   defn (fn defn [&form &env name & fdecl]
          ;; Note: Cannot delegate this check to def because of the call to (with-meta name ..)
          (if (instance? clojure.lang.Symbol name)
            nil
            (throw (IllegalArgumentException. "First argument to defn must be a symbol")))
          (let [m (if (string? (first fdecl))
                    {:doc (first fdecl)}
                    {})
                fdecl (if (string? (first fdecl))
                        (next fdecl)
                        fdecl)
                m (if (map? (first fdecl))
                    (conj m (first fdecl))
                    m)
                fdecl (if (map? (first fdecl))
                        (next fdecl)
                        fdecl)
                fdecl (if (vector? (first fdecl))
                        (list fdecl)
                        fdecl)
                m (if (map? (last fdecl))
                    (conj m (last fdecl))
                    m)
                fdecl (if (map? (last fdecl))
                        (butlast fdecl)
                        fdecl)
                m (conj {:arglists (list 'quote (sigs fdecl))} m)
                m (let [inline (:inline m)
                        ifn (first inline)
                        iname (second inline)]
                    ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
                    (if (if (clojure.lang.Util/equiv 'fn ifn)
                          (if (instance? clojure.lang.Symbol iname) false true))
                      ;; inserts the same fn name to the inline fn if it does not have one
                      (assoc m :inline (cons ifn (cons (clojure.lang.Symbol/intern (.concat (.getName ^clojure.lang.Symbol name) "__inliner"))
                                                       (next inline))))
                      m))
                m (conj (if (meta name) (meta name) {}) m)]
            (list 'def (with-meta name m)
                  ;;todo - restore propagation of fn name
                  ;;must figure out how to convey primitive hints to self calls first
  								;;(cons `fn fdecl)
  								(with-meta (cons `fn fdecl) {:rettag (:tag m)})))))
  
  (. (var defn) (setMacro))
  ```
- Seems straightforward enough. The first thing I see is it uses a named `fn`, which we don't have a concept of. I think it might be fine to just skip it.
- Joy of Clojure includes an explanation.
- This works:
- ```clojure
  (defmacro defn
    (fn [name arglist & value]
      `(def ~name (fn ~arglist ~@value))))
  ```
- Wait, does that need to be a macro? Yes, otherwise it like, evaluates the function instead of defining it or something.
- ## Keyword functions
	- So we have the basic functionality of calling a keyword to do an implicit `get` on a map or vector. But what about things like this:
	- ```clojure
	  ((juxt :a :c :b) {:a 2, :b 4, :c 6, :d 8 :e 10}))
	  ```
	- It works if I explicitly make them functions:
	- ```clojure
	  (juxt #(get % :a) #(get % :c) #(get % :b)) 
	     {:a 2, :b 4, :c 6, :d 8 :e 10})
	  ```
	- But it would be cool to make it work the other way.
	- It could have something to do with the implementation of `juxt`:
	- ```clojure
	  (defn juxt [& f]
	    (fn [& a]
	      (map #(apply % a) f)))
	  ```
- ## loop
- The clojure source is surprisingly simple:
- ```clojure
  (defmacro loop [bindings & body]
      (assert-args
        (vector? bindings) "a vector for its binding"
        (even? (count bindings)) "an even number of forms in binding vector")
      (let [db (destructure bindings)]
        (if (= db bindings)
          `(loop* ~bindings ~@body)
          (let [vs (take-nth 2 (drop 1 bindings))
                bs (take-nth 2 bindings)
                gs (map (fn [b] (if (symbol? b) b (gensym))) bs)
                bfs (reduce1 (fn [ret [b v g]]
                              (if (symbol? b)
                                (conj ret g v)
                                (conj ret g v b g)))
                            [] (map vector bs vs gs))]
            `(let ~bfs
               (loop* ~(vec (interleave gs gs))
                 (let ~(vec (interleave bs gs))
                   ~@body)))))))
  ```
- It uses `loop*` which I have no idea what it is.
- I reimplemented the previous loop/recur special forms.
- This works:
- ```clojure
  (loop [xs (seq [1 2 3 4 5])
         result []]
    (if xs
        (recur (next xs) 
          (conj result (* (first xs) (first xs))))
      result))
  => [1 4 9 16 25]
  ```
- But it wouldn't work with a nested let binding:
- ```clojure
  (loop [xs (seq [1 2 3 4 5])
         result []]
    (if xs
      (let [x (first xs)]
        (recur (next xs) (conj result (* x x))))
      result))
  => 
  Error: 'x' not found
  ```
- I think I know why! It's because it's a `let` inside a `loop`.
- I also had this problem in a macro I think.
- Let's make a [[step debugger]]
- It's cool that I can solve problems using js interop, like this solution to hamming:
- ```clojure
  (defn distance [strand1 strand2]
    (if-not (= (count strand1) (count strand2)) nil
      (js-eval (str "[..." "'" strand1 "'"
                 "].filter((element, index) => element !== "
                 "'" strand2 "'" "[index]).length"))))
  ```
- But it's a bit cheesy, i.e. as long as I'm getting tests to pass this way, I'm not actually improving the language...
- ```clojure
  (defn valid? [s]
    (let [pairs {")" "(" "]" "[" "}" "{"}
          opening (set (vals pairs))
          closing (set (keys pairs))]
      (loop [stack [] s s]
        (cond (empty? s) (empty? stack)
              (contains? opening (first s)) (recur (conj stack (first s)) (rest s))
              (contains? closing (first s)) (if (= (peek stack) (get pairs (first s)))
                            (recur (pop stack) (rest s))
                            false)
              :else (recur stack (rest s))))))
  ```
- # hash-maps
	- A big limitation currently is that hash map keys have to be strings, because they're plain objects. I got around this before by using immutable-js. But I'd like to figure out how to do it without it.
	- Ah, there's a `Map` type in javascript. Why haven't I been using that?
	- God damn, this is annoying. Everything seems like it should be fine but literal hashmaps are not printing. Somehow they're being turned back into objects but I can't figure it out.
	- Even this works:
	- ```clojure
	  
	  ```
	- Fuck. I hate this
	- It's wrong even if I disconnect the printer, it outputs as `[object Object]` instead of `[object Map]`.
	- The reader is fine. If I just output the reader I get `{1 5} => [object Map] `
	- So what is happening during evaluation?
	- It's `eval_ast!` omg, finally! I figured it out!
	- I must need to tie up some other stuff though, because many tests are failing which passed before.
	- I had to fix `get`.
- # map
	- ```clojure
	  (defn map
	    "Returns a sequence of the result of applying f to
	    the set of first items of each coll, followed by applying f to the
	    set of second items in each coll, until any one of the colls is
	    exhausted.  Any remaining items in other colls are ignored. Function
	    f should accept number-of-colls arguments."
	    ([f]
	      (fn [rf]
	        (fn
	          ([] (rf))
	          ([result] (rf result))
	          ([result input]
	             (rf result (f input)))
	          ([result input & inputs]
	             (rf result (apply f input inputs))))))
	    ([f coll]
	     (lazy-seq
	      (when-let [s (seq coll)]
	        (if (chunked-seq? s)
	          (let [c (chunk-first s)
	                size (int (count c))
	                b (chunk-buffer size)]
	            (dotimes [i size]
	                (chunk-append b (f (.nth c i))))
	            (chunk-cons (chunk b) (map f (chunk-rest s))))
	          (cons (f (first s)) (map f (rest s)))))))
	    ([f c1 c2]
	     (lazy-seq
	      (let [s1 (seq c1) s2 (seq c2)]
	        (when (and s1 s2)
	          (cons (f (first s1) (first s2))
	                (map f (rest s1) (rest s2)))))))
	    ([f c1 c2 c3]
	     (lazy-seq
	      (let [s1 (seq c1) s2 (seq c2) s3 (seq c3)]
	        (when (and  s1 s2 s3)
	          (cons (f (first s1) (first s2) (first s3))
	                (map f (rest s1) (rest s2) (rest s3)))))))
	    ([f c1 c2 c3 & colls]
	     (let [step (fn step [cs]
	                   (lazy-seq
	                    (let [ss (map seq cs)]
	                      (when (every? identity ss)
	                        (cons (map first ss) (step (map rest ss)))))))]
	       (map #(apply f %) (step (conj colls c3 c2 c1))))))
	  ```
- There's a problem, when a parameter is called as a function, for example in `take-while`:
- ```clojure
  (defn take-while [pred coll]
    (loop [s (seq coll) res []]
      (if (empty? s) res
          (if (pred (first s))
            (recur (rest s) (conj res (first s)))
            res))))
  ```
- It works when used by itself, but when called by *another* function, it doesn't have `pred` defined in its env. This happens in `partition-by`:
- ```clojure
  (defn partition-by [f coll]
    (loop [s (seq coll) res []]
      (if (= 0 (count s)) res
          (recur (drop (count (take-while (fn [x] (= (f (first s)) (f x))) s)) s)
                 (conj res (take-while (fn [x] (= (f (first s)) (f x))) s))))))
  ```
- I don't really get it. Maybe related to the problem with loop scope. I don't completely get how functions work. Maybe I will eventually...
- # Mulitiarity - Joy of Clojure
	- That's right... look what we have on page 190: *Putting it all together: macros returning functions*
- # Scope issues
	- Let's see if I can minimally reproduce the scope issue(s).
	- I wouldn't be surprised if something is wonky with loop/let because the whole thing seems kind of weird. Let's review it now.
	- There are global vars for the `loopVars`, `loopAST` and `loop_env.` I already don't like it. I don't think it's ok to have just one loop env but I've been unable to rationalize it so far.
	- ```js
	  case "loop":
	    loopVars = []
	    loop_env = new Env(env)
	    loopAST = ast.slice(2)
	    for (var i = 0; i < a1.length; i += 2) {
	      loop_env.set(a1[i], EVAL(a1[i + 1], loop_env))
	      loopVars.push(a1[i])
	     }
	    ast = a2;
	    env = loop_env;
	  break;
	  ```
	- Maybe it is fine, because it creates a new loop_env each time. But what about the case where a function is called inside a loop which also has a loop... it would wipe out the locals of the first loop. Except that each loop env creates contains the previous env as its `outer` env.
- ## assoc bug with sets
	- This fails:
	- ```clojure
	  (group-by set ["meat" "team" "eat"])
	  => {#{"m" "e" "a" "t"} nil #{"t" "e" "a" "m"} nil #{"e" "a" "t"} nil}
	  ```
	- There's a couple of big problems. Clojure does it right:
		- ```clojure
		  (group-by set ["meat" "team" "eat"])
		  => {#{\a \e \m \t} ["meat" "team"], #{\a \e \t} ["eat"]}
		  ```
	- Hmm, maybe it works because it sorts the set.
	- So now I'm sorting the set in `assoc`, but it still does the wrong thing:
	- ```clojure
	  (group-by set ["meat" "team" "eat"])
	  => {#{"a" "e" "m" "t"} nil #{"a" "e" "m" "t"} nil #{"a" "e" "t"} nil}
	  ```
	- Let's remove `group-by` from the question. We'll deal with that later.
	- It's still a bug in assoc
	- ```clojure
	  (assoc {#{\a \e \m \t} ["hi"]} #{\t \e \a \m} ["boop"])
	  => {#{\a \e \m \t} ["hi"] #{\a \e \m \t} ["boop"]}
	  ```
	- So sorting it isn't enough. I think it's part of how equality works for javascript map keys.
- # Protocols
- ```clojure
  (defprotocol AProtocol
    (bar [a b] "bar docs")
    (baz [a] [a b] [a b c] "baz docs"))
  ```
- Meh, I don't really care at this point. I'd like to try debugging my loop env issue, whatever it is.
- # loop/let/env issue
- I want to come up with a simple example that foils my current implementation. It shouldn't be hard.
- Here's one:
- ```clojure
  (loop [x [1 2 3] res []]
    (let [y 2]
      (if (empty? x) res
        (recur (rest x) (conj res (* y (first x)))))))
  => 
  Error: 'y' not found
  ```
- Now that's actually surprising, I'd expect that to work. Would be good to get to the bottom of this.
- The problem is when we hit recur, the let env is not being used. In fact, the function body is not evaluated, only the actual forms inside `recur`. It seems so obvious now. But I'm not sure how to fix it.
- The `let_env` still exists. I could walk the ast and see if there's a `let` in it, and if so, create a *new* env with the `loop_env` as outer.
- It works! But I can't merge it at this point because it will have unexpected side effects if there's not a let outside the recur, in which case it will use the locals from the previous let if there is one.
- I think it's time to dig up my trusty `walk` function. We have the loopAST.
- It works! Now to test that it doesn't do the wrong thing:
- ```clojure
  (let [x 9])
  
  (loop [x [1 2 3] res []]
      (if (empty? x) res
        (recur (rest x) (conj res (first x)))))
  => [1 2 3]
  ```
- If the check wasn't done, it would have clobbered the `x` local. Nice!
- ```clojure
  `(defn pack [s]\r\n  (loop [s (rest s)\r\n				  prev (first s)\r\n				  res []\r\n				  subseq [(first s)]](if (seq s)\r\n			  (let [f (first s)]\r\n				 (if (= f prev)\r\n					(recur (rest s) f res (conj subseq f))\r\n					(recur (rest s) f (conj res (seq subseq)) [f])))\r\n			
                                                                                                                                                 (conj res (seq subseq)))))`
  ```
- [[Clojure interpreter notes page 9]]
-