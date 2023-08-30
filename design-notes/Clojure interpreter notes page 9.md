- How come this works:
- ```clojure
  (defmacro defn [name & fdecl]
    (if (string? (first fdecl))
      `(def ~name (fn ~(second fdecl) (do ~@(rest (rest fdecl)))))
      `(def ~name (fn ~(first fdecl) (do ~@(rest fdecl))))))
  ```
- But not this:
- ```clojure
  (defmacro defn [name & fdecl]
    (if (string? (first fdecl))
      `(do (def ~name (fn ~(second fdecl) (do ~@(rest (rest fdecl)))))
           (def ~name ~(with-meta ~name {:doc (first fdecl)})))
      `(def ~name (fn ~(first fdecl) (do ~@(rest fdecl))))))
  ```
- Test code:
- ```clojure
  (defn hi
    "my docstring"
    []
   (for [x (range 1 6)
        :let [y (* x x)
              z (* x x x)]]
    [x y z]))
  
  (meta hi) => {:doc "my docstring"} 
  (hi)
  => ([1 1 1] [2 4 8] [3 9 27] [4 16 64] [5 25 125] nil)
  ```
- Wait... this is the way it works (notice slight diff)
- ```clojure
  (defmacro defn [name & fdecl]
    (if (string? (first fdecl))
      `(do (def ~name (fn ~(second fdecl) (do ~@(rest (rest fdecl)))))
          (def ~name (with-meta ~name ~{:doc (first fdecl)})))
      `(def ~name (fn ~(first fdecl) (do ~@(rest fdecl))))))
  ```
- We need `get-in` and friends.
- ```clojure
  (defn merge-with [f & maps]
    (when (some identity maps)
      (let [merge-entry (fn [m e]
                          (let [k (key e) v (val e)]
                            (if (contains? m k)
                              (do (println "doing")
                                (assoc m k (f (get m k) v)))
                              (do (println "not doing")
                                (assoc m k v)))))
            merge2 (fn [m1 m2]
                     (reduce merge-entry (or m1 {}) (seq m2)))]
        (reduce merge2 maps)))
    maps)
  
  (merge-with + {:a 1} {:a 2} {:a 3})
  
  (assoc {:a 1} :b 2 :c 3)
  
  (get {:a 1} :a)
  
  (seq {:a 1 :b 2})
  
  (key (first (seq {:a 1 :b 2})))
  
  (merge-with * {:a 2, :b 3, :c 4} {:a 2} {:b 2} {:c 5})
  
  (merge-with +
                 {:a 1  :b 2}
                 {:a 9  :b 98 :c 0})
  
  (merge-with + {:a 1} {:a 2} {:a 3})
  ```
- ```clojure
  (defn ttt [v x]
    (into #{}
       (filter
         (fn [d]
           (and (= (get-in x d) :e)
             (let [a (assoc-in x d v) c (second (second a))]
               (some #(= #{v} (set %))
                 (concat (apply map vector a)
                   (conj a (vector (ffirst a) c (nth (nth a 2) 2))
                     (vector (nth (nth a 0) 2) c (nth (nth a 2) 0))))))))
         (for [i (range 3) j (range 3)] [i j]))))
  ```
- # Iterate
	- There's a perfect construct for this right in javascript... iterators. In fact, here is one making an infinite range of integers:
	- ```js
	  function makeRangeIterator(start = 0, end = Infinity, step = 1) {
	    let nextIndex = start;
	    let iterationCount = 0;
	  
	    const rangeIterator = {
	      next() {
	        let result;
	        if (nextIndex < end) {
	          result = { value: nextIndex, done: false };
	          nextIndex += step;
	          iterationCount++;
	          return result;
	        }
	        return { value: iterationCount, done: true };
	      },
	    };
	    return rangeIterator;
	  }
	  ```
	- The [documentation](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Iterators_and_Generators) goes on to explain generators, which it seems are more friendly to use as opposed to less.
	- > Generator functions ... allow you to define an iterative algorithm by writing a single function
	- It goes on to basically describe lazy seqs.
	- Here is the same range iterator, but using a generator functionn
	- ```js
	  function* makeRangeIterator(start = 0, end = Infinity, step = 1) {
	    let iterationCount = 0;
	    for (let i = start; i < end; i += step) {
	      iterationCount++;
	      yield i;
	    }
	    return iterationCount;
	  }
	  ```
	- Looks good, love it!
	- It's in my code. Now I just need to wire it up to everything
	- ok so now how do I make an arbitrary iterator function?
- `apply` is doing weird things
- ```clojure
  (apply + ()) => 0/1 
  (apply * ()) => 1/1 
  ```
- Could this have something to do with adding ratios?
- What's really strange is that in the MAL web repl it returns NaN for both.
- # Destructuring with JavaScript?
	- Here's an idea. Since js has its own destructuring, maybe we could take advantage of that?
	- Put a pin in that for now. I'm taking another look at the clojure function and I think I made a big step towards understanding it. I managed to break apart the functions inside it, and there is:
		- 1. pvec
		  2. pmap
		  3. pb
	- And if we take a look at `pb`, it all comes into focus:
	- ```clojure
	  (defn pb [bvec b v]
	      (cond
	        (symbol? b) (-> bvec (conj (if (namespace b)
	                                     (symbol (name b)) b)) (conj v))
	        (keyword? b) (-> bvec (conj (symbol (name b))) (conj v))
	        (vector? b) (pvec bvec b v)
	        (map? b) (pmap bvec b v)
	        :else (throw (str "Unsupported binding form: " b))))
	  ```
	- It suddenly doesn't seem so bad! Let's just start with pvec. That's the one used most often anyway.
	- The action of the `destructure` fn:
	- ```clojure
	  (reduce process-entry [] bents)
	  ```
	- `process-entry`, the reducing fn:
	- ```clojure
	  (fn [bvec b] (pb bvec (first b) (second b)))
	  ```
	- So `pb` is called on `[]` and `(first bents)`, the first coll of our partitioned bindings.
	- For a simple binding like `'[[a b] ["a" "b"]]`, it's done in a single iteration, i.e. we don't even need to call `reduce`:
	- ```clojure
	  (pb bvec (first b) (second b))
	  [vec__25317
	   ["a" "b"]
	   a
	   (#object[clojure.core$nth 0x75cd3577 "clojure.core$nth@75cd3577"] vec__25317 0 nil)
	   b
	   (#object[clojure.core$nth 0x75cd3577 "clojure.core$nth@75cd3577"] vec__25317 1 nil)]
	  ```
	- Since the first of the binding pairs is a vector, we call
	- ```clojure
	  (pvec [] (first b) (second b))
	  ```
	- And here we get into the meat of the operation, `pvec`. It fits neatly on one screen:
	- ```clojure
	  (defn pvec [bvec b val]
	    (let [gvec (gensym "vec__")
	          gseq (gensym "seq__")
	          gfirst (gensym "first__")
	          has-rest (some #{'&} b)]
	      (loop [ret (let [ret (conj bvec gvec val)]
	                   (if has-rest
	                     (conj ret gseq (list seq gvec))
	                     ret))
	             n 0
	             bs b
	             seen-rest? false]
	        (if (seq bs)
	          (let [firstb (first bs)]
	            (cond
	              (= firstb '&) (recur (pb ret (second bs) gseq)
	                                   n
	                                   (nnext bs)
	                                   true)
	              (= firstb :as) (pb ret (second bs) gvec)
	              :else (if seen-rest?
	                      (throw "Unsupported binding form, only :as can follow & parameter")
	                      (recur (pb (if has-rest
	                                   (conj ret
	                                         gfirst `(~first ~gseq)
	                                         gseq `(~next ~gseq))
	                                   ret)
	                                 firstb
	                                 (if has-rest
	                                   gfirst
	                                   (list nth gvec n nil)))
	                             (inc n)
	                             (next bs)
	                             seen-rest?))))
	          ret))))
	  ```
	- The best part is... there's no destructuring, hahaha
	- We create 3 gensyms, `vec`, `seq` and `first`. Then we initialize a loop.
	- Ah, I found out why mine was failing already! It's this:
	- ```clojure
	  (some #{'&} b) => 
	  Error: f.apply is not a function 
	  ```
	- Because I haven't implemented `some` on sets. let's do it!
	- Done, but for some reason this doesn't work:
	- ```clojure
	  (some #{'&} ['&]) => nil 
	  ```
	- And I even tried `(some #{(symbol "&")} [(symbol "&")])`, so there's a bug in `contains?` or something that breaks when operating on a symbol.
	- I can probably detect it some other way.
	- String interop hack it is
	- ```clojure
	  (defn has-rest? [b]
	    (js-eval (str "'" b "'" ".includes(" "'" "&" "'" ")")))
	  ```
	- Alright, so initialize the loop variables. We have `ret`:
	- ```clojure
	  (let [ret (conj bvec gvec val)]
	                   (if has-rest
	                     (conj ret gseq (list seq gvec))
	                     ret))
	  ```
	- Cool, we can see the beginning of our new binding:
	- ```clojure
	  (conj bvec gvec val)
	  => [vec__4 ["a" "b"]]
	  ```
	- Fortunately, equality works on symbols: `(= '& '&) => true`
	- Everything's good until `recur`. Then it calls back into `pb`:
	- ```clojure
	  (pb (if has-rest
	        (conj ret
	              gfirst `(~first ~gseq)
	              gseq `(~next ~gseq))
	        ret)
	      firstb
	      (if has-rest
	        gfirst
	        (list nth gvec n nil)))
	  ```
	- So this could be a problem with mutual recurring loops. I still suspect there may be a problem with that, since there's a global loop_env.
	- This is the line that breaks:
	- ```clojure
	  (list nth gvec n nil) => 
	  Error: Cannot read properties of undefined (reading 'get') 
	  ```
	- Ah... all it needs is a quote:
	- ```clojure
	  (list 'nth gvec n nil) => (nth vec__13 0 nil) 
	  ```
	- By not quoting it is how Clojure gives `(#object[clojure.core$nth 0x75cd3577 "clojure.core$nth@75cd3577"] gvec 0 nil)`. There must be some reason for this but I don't know what it could be.
	- It fucking works!
	- ```clojure
	  (destructure '[[a b] ["a" "b"]])
	  => [vec__14 ["a" "b"] a (nth vec__14 0 nil) b (nth vec__14 1 nil)] 
	  ```
	- It fails on a simple vector like so
	- ```clojure
	  (destructure '[x 2]) => 
	  Error: Maximum call stack size exceeded
	  ```
	- The good news is, I changed the `let` in the interpreter back to `let*`, and made a macro called `let` that calls `let*` with `destructure`. So once this works, it will work for `for` as well.
	- Hooking it up caused 3 tests to fail. I wonder which ones they were. But it's merged. Now I just need to do it for maps. That function is a bit more complicated.
	- # fixing `loop`
	- ```js
	  const walked = postwalk(x => {
	                      console.log("walking:", x)
	                      if (x.value === 'recur') {
	                          x.locals = locals
	                      }
	                  }, ast)
	  ```
	- I've sketched out how this should work... the key word is *metadata*. It's essentially the same thing it's doing for functions. At `loop`, we store the AST right on the `recur` symbol so when it is called, it knows which vars need to be reset and the form to return to. Pretty simple, but I'm struggling to implement it as usual. I don't think there's any problem, I'm just fumbling to find the right combination of calls. It will be well worth it, because then the interpreter will be rock solid.
	- It's doing something funny that I hadn't anticipated. I was confused why it seems to be binding too many loop variables... it starts out perfectly, but then when it goes back into the inner loop function for the second time... it thinks it's a new loop! How silly of me not to realize that we need to have some way of resuming a loop that is already in execution that we are returning to! So no problem... we'll say, have another property on the loop symbol designating it as started, completed or whatever, so we don't create new variables when it's already mid-execution.
	- ok making a whole page for this... I want to get to the bottom of [[loop]] today
	-
- [[Clojure interpreter notes page 10]]