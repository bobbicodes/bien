- `mapcat` doesn't work:
- ```clojure
  (mapcat reverse [[3 2 1 0] [6 5 4] [9 8 7]]) => 
  Error: f.apply is not a function 
  ```
- `mapcat` definition:
- ```clojure
  (defn mapcat [f & colls]
    (apply concat (apply map f colls)))
  ```
- The problem is not `mapcat`. See:
- ```clojure
  (apply map reverse [[3 2 1 0] [6 5 4] [9 8 7]]) => 
  Error: seq: called on non-sequence 
  ```
- It's not even getting past `map`:
- ```clojure
  (map reverse [[3 2 1 0] [6 5 4] [9 8 7]]) => 
  Error: f.apply is not a function 
  ```
- Many of the examples on ClojureDocs work, so at least it's not critically wrong.
- Even this one:
- ```clojure
  (apply map vector [[:a :b :c]
                     [:d :e :f]
                     [:g :h :i]])
  => [[:a :d :g] [:b :e :h] [:c :f :i]] 
  ```
- Coming back to the function at hand, the individual calls work:
- ```clojure
  (reverse (first [[3 2 1 0] [6 5 4] [9 8 7]]))
  => (0 1 2 3) 
  ```
- It also fails using the specific arity of map
- ```clojure
  (map1 reverse [[3 2 1 0] [6 5 4] [9 8 7]]) => 
  Error: f.apply is not a function 
  ```
- Impl of `map1`:
- ```clojure
  (defn map1 [f coll]
    (loop [s (seq coll) res []]
      (if (empty? s) res
          (recur (rest s) (conj res (f (first s)))))))
  ```
- ok, coming in for a landing. It's a problem with loop, unfortunately.
- ```clojure
  (loop [s (seq [[3 2 1 0] [6 5 4] [9 8 7]]) res []]
      (if (empty? s) res
          (recur (rest s) (conj res (reverse (first s)))))) => 
  Error: f.apply is not a function 
  ```
- I see... sort of. It's trying to evaluate a list which is returned by reverse.
- If I have reverse return a vector, it works. But I shouldn't have to do that. It must be evaluating the forms twice.
- I fixed it, but now I'm mysteriously getting the wrong output:
- ```clojure
  (loop [s (seq [[3 2 1 0] [6 5 4] [9 8 7]]) res []]
    (if (empty? s) res
        (recur (rest s) (conj res (reverse (first s))))))
  => [(4 5 6) (7 8 9) ()] 
  ```
- Oh I have an idea. I had this problem before. This is *exactly* the same problem:
- ```clojure
  (loop [a 3 b []]
    (if (= a 0)
      b
      (recur (dec a) (conj b a))))
  => [2 1 0] 
  ```
- I solved it before by... think, think, think.... capturing the recur forms and evaluating them, and then setting the bindings, instead of evaluating them in order as they are set in the env.
- I did it! I fucking did it!
- Now there are 92 tests passing! That's the most ever!!!!
- largest_series_product takes a hella long time. I thought it was crashed. Also gigasecond. Probably because they errored before.
- Trying to implement `thrown?`
- ```clojure
  (thrown? (largest-product 1 ""))
  ```
- I don't know how to do it, because once it's thrown the execution stops.
- Gigasecond isn't taking long because of a long running test. It takes long because it has to calculate 1000000000 seconds... oh well, it only takes like 5 seconds. I'll discard 2 of the three tests to speed it up.
- Hey guess what? now over half of the tests are passing, 92-93.
- `for` completely works now!
- ```clojure
  (for [x [0 1 2 3 4 5]
        :let [y (* x 3)]
        :when (even? y)]
    y) => (0 6 12) 
  ```
- ```clojure
  (defn prime? [n]
    (not-any? zero? (map #(rem n %) (range 2 n))))
  
  (for [x (range 3 33 2) :when (prime? x)]
           x)
  => (3 5 7 11 13 17 19 23 29 31) 
  ```
- But if I remove `(remove nil?)`, there are still nils
- destructuring is currently broken though. That's probably the biggest wart atm
- 101 tests passing! I implemented `condp`
- ## `case`
	- I don't know why, but `case` is way more complicated than I expected. But in the process I've implemented bit shifts, so there's that. But why the fuck does `case` need bit shift?
	- Oh, and `case` merely prepares a sorted map to be passed to `case*` which is a compiler built-in. Joy.
	- It's the very last thing in the compiler.
	- I thought it would be just like `cond`! wtf?!?!
- There's a question on Clojureverse from Shaun Lebron about resources for learning about the Clojure compiler. Alex Miller gave some video links! Let's check 'em out! https://clojureverse.org/t/resources-for-learning-how-the-clojure-compiler-works/9059
- Brainstorming [[function arities]]
- glad that's done and seems to be working
- time for [[Clojure interpreter notes page 12]]