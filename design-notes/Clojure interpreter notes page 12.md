- ## Keyword functions
	- There is more to this feature than creating an implicit call to `get` when used in function position, i.e. `(:a {:a 1}) => 1 ` or `({:a 1} :a) => 1 `.
	- Both of those work which is great, but what about something like
	- ```clojure
	  ((juxt :a :b) {:a 1 :b 2 :c 3 :d 4})
	  => [1 2]
	  ```
	- If we try that in ours, we get
	- ```clojure
	  ((juxt :a :b) {:a 1 :b 2 :c 3 :d 4})
	  => 
	  Error: f.apply is not a function 
	  ```
	- Let's try to figure out why this happens.
	- Our `juxt` is delightfully spartan compared to the real one, thanks to 4clojure users whose solutions provided it.
	- ```clojure
	  (defn juxt [& f]
	    (fn [& a]
	      (map #(apply % a) f)))
	  ```
	- Given the error message, it seems reasonable to assume that it's the call to `apply` that causes it. Let's try to address it there.
	- Our apply is just JavaScript's apply:
	- ```js
	  function apply(f) {
	      var args = Array.prototype.slice.call(arguments, 1);
	      return f.apply(f, args.slice(0, args.length - 1).concat(args[args.length - 1]));
	  }
	  ```
	- Clojure accomplishes this via the IFn interface.
	- Funny enough, the only example that uses keywords on the clojuredocs page for `apply` does it in a fashion that works for ours:
	- ```clojure
	  (def entries [{:month 1 :val 12}
	                {:month 2 :val 3}
	                {:month 3 :val 32}])
	  
	  (apply max (map
	              #(:val %)
	              entries))
	  => 32
	  ```
	- Where in Clojure it could have just been
	- ```clojure
	  (apply max (map
	              :val
	              entries))
	  ```
	- I wonder why `(map :val entries)` doesn't work, because map uses `f` in the function position:
	- ```clojure
	  (defn map1 [f coll]
	    (loop [s (seq coll) res []]
	      (if (empty? s) res
	          (recur (rest s) (conj res (f (first s)))))))
	  ```
	- I would expect it to merely be a loop of
	- ```clojure
	  (:val (first entries))
	  => 12 
	  ```
	- It works if I handle it in `map`:
	- ```clojure
	  (defn map1 [f coll]
	    (loop [s (seq coll) res []]
	      (if (empty? s) res
	          (recur (rest s) (conj res 
	                                (if (keyword? f)
	                                  (get (first s) f)
	                                  (f (first s))))))))
	  
	  (map :val entries)
	  => [12 3 32] 
	  ```
	- So that's cool... still not sure why it didn't just work.
	- `apply` works if I do this:
	- ```clojure
	  (defn apply [f coll]
	    (if (keyword? f)
	      (get coll f)
	      (apply* f coll)))
	  ```
	- But for some reason that isn't enough for `juxt`:
	- ```clojure
	  ((juxt :a :b) {:a 1 :b 2 :c 3 :d 4})
	  => 
	  Error: coll.get is not a function 
	  ```
	- This might actually be the fault of our super tiny implementation of `juxt`.
	- I have a solution... but it causes like 5 tests to fail.
	- wait... no it was something else that lost those.
	- This is the solution:
	- ```js
	  else {
	      if (types._keyword_Q(f)) {
	          return EVAL([f].concat(el.slice(1)), env)
	      }
	      var res = f.apply(f, el.slice(1));
	      return res
	  }
	  ```
- Cool! I'd say this was a success in spite of the few casualties, whatever they are.
- I just gained one by solving `say`. That's right, say! I needed to filter nils when calling `str`, and fix one of the functions that I messed up while removing destructuring... by putting back the function with destructuring!!!!
- This example of `comp` fails:
- ```clojure
  (#((apply comp first (repeat %2 rest)) %1) [1 2 3 4 5 6] 3)
  ```
- Also this case, from 4clojure, which should return `true`:
- ```clojure
  ((mycomp zero? #(mod % 8) +) 3 5 7 9)
  => false 
  ```
- ```clojure
  (defn apply
    "Applies fn f to the argument list formed by prepending intervening arguments to args."
    {:added "1.0"
     :static true}
    ([^clojure.lang.IFn f args]
       (. f (applyTo (seq args))))
    ([^clojure.lang.IFn f x args]
       (. f (applyTo (list* x args))))
    ([^clojure.lang.IFn f x y args]
       (. f (applyTo (list* x y args))))
    ([^clojure.lang.IFn f x y z args]
       (. f (applyTo (list* x y z args))))
    ([^clojure.lang.IFn f a b c d & args]
       (. f (applyTo (cons a (cons b (cons c (cons d (spread args)))))))))
  ```
- This call to `apply` should work:
- ```clojure
  (apply map list (partition 2 [1 2 3 4 5 6]))
  ```
- But we haven't implemented any other arities for `apply`:
- ```clojure
  (defn apply [f coll]
    (if (keyword? f)
      (get coll f)
      (apply* f coll)))
  ```
- Done! And now 107 tests are passing!
- `diamond` is failing because of this use of `iterate`:
- ```clojure
  (iterate (partial + 2) 1)
  ```
- `iterate` returns my little `Iterate` object, which is an ES6 class:
- ```js
  class Iterate {
      constructor(f, x) {
          this.name = 'Iterate'
          this.f = f
          this.realized = [x];
      }
      next() {
          this.realized.push(this.f(this.realized[this.realized.length-1]))
          return this.realized; 
      }
  }
  ```
- So the actual question is what happens after that, how is it to be consumed?
- It eventually becomes a call to `take`:
- ```clojure
  (defn row-paddings [num-letters]
    (let [inner-padding (conj (iterate (partial + 2) 1) 0)
          outer-padding (iterate dec (dec num-letters))]
      (take num-letters (map vector inner-padding outer-padding))))
  ```
- It doesn't make it past `(conj (iterate (partial + 2) 1) 0)`
- `conj` needs to know how to operate on it.
- Here's how it works with `take`:
- ```js
  if (types._iterate_Q(coll)) {
      for (let i = 0; i < n; i++) {
          coll.next()
      }
      return coll.realized.slice(0, -1)
  }
  ```
- While vector functions work, it fails if it's something that evaluates to a vector:
- ```clojure
  (def v [1 2 3])
  ([1 2 3] 0) => 1 
  (v 0)
  => 
  Error: f.apply is not a function 
  ```
- Fixed, similar to keywords, and did maps as well
- `keys` and `vals` don't work.
- Want to implement clojure.walk in Clojure
- wtf is the deal here
- ```clojure
  (defn retain [pred coll]
    (filter pred coll))
  
  (defn discard [pred coll]
    (remove pred coll))
  
  (discard zero? [0 1 2]) => nil
  
  (remove zero? [0 1 2]) => (1 2) 
  ```
- ## Sort-by
- Clojurescript might be help here:
- ```clojure
  (defn ^number compare
    "Comparator. Returns a negative number, zero, or a positive number
    when x is logically 'less than', 'equal to', or 'greater than'
    y. Uses IComparable if available and google.array.defaultCompare for objects
   of the same type and special-cases nil to be less than any other object."
    [x y]
    (cond
     (identical? x y) 0
     (nil? x) -1
     (nil? y) 1
     (number? x) (if (number? y)
                   (garray/defaultCompare x y)
                   (throw (js/Error. (str "Cannot compare " x " to " y))))
     (satisfies? IComparable x)
     (-compare x y)
     :else
     (if (and (or (string? x) (array? x) (true? x) (false? x))
              (identical? (type x) (type y)))
       (garray/defaultCompare x y)
       (throw (js/Error. (str "Cannot compare " x " to " y))))))
  ```
- ```clojure
  (defn ^:private fn->comparator
    "Given a fn that might be boolean valued or a comparator,
     return a fn that is a comparator."
    [f]
    (if (= f compare)
      compare
      (fn [x y]
        (let [r (f x y)]
          (if (number? r)
            r
            (if r
              -1
              (if (f y x) 1 0)))))))
  ```
- [[Clojure interpreter notes page 13]]
-