- # Zippers!
	- The only thing I need is keyword functions. Let's try that!
	- Done! And I added everything else necessary to solve the zipper exercise!
	- Made it into a separate library, and made a primitive require system.
- # Lazy sequences
	- Here's where I might be able to benefit from a library. This looks really good: https://github.com/beark/lazy-sequences
	- But I don't know how I can use it. There's an `iterate` method that takes an arbitrary function and produces a lazy seq. But how do I get from that to being able to pass a potentially infinite sequence to it?
	- Clojurescript implements them using protocols. And I have an implementation of protocols! It's from Chouser: https://gist.github.com/Chouser/6081ea66d144d13e56fc
	- I says it's a "sketch"... so I'm not sure how complete it is. There is a working example!
	- I'm learning about JavaScript's generators, which might be able to accomplish this natively.
	- How about immutable.js? That has lazy seqs, and probably a bunch more stuff we could use.
	- I implemented `range` to use immutable.js's Range objects. Now I have to make all the things use it.
	- Infinite sequences work!
	- ```clojure
	  (take 4 (range))
	  => (0 1 2 3)
	  ```
	- The weird thing is, I didn't even have to modify `take` for that to work. `take` uses normal `slice()`.
	- The tests are making the whole page crash. It was on `armstrong-numbers` when it got stuck:
	- ```clojure
	  (defn expt [base pow]
	    (reduce * 1 (repeat pow base)))
	  
	  (defn armstrong? [n]
	    (let [digits (map #(read-string (str %)) (str n))
	          l      (count digits)]
	      (= n (reduce + 0 (map #(expt % l) digits)))))
	  ```
	- The problem is in this solution - just need to track down exactly what.
	- oooh, elusive. It doesn't fail if I eval it in pieces. I suspect something is failing because a seq is printing a list, but doesn't behave like one.
	- Ah. The output of `count` is wrong. well, not wrong... but the seq cannot apparently be counted the way `count` is doing it.
	- Well I got it so it doesn't hang anymore... it just fails
	- Fixed it! There are several other exercises that are failing now that were passing before. I'll have to figure out which ones
	- These ones are passing:
	- 1. armstrong_numbers',
	  2.  'two_fer',
	  3.  'grains',
	  4.  'word_count',
	  5.  'difference_of_squares',
	  6.  'hello_world',
	  7.  'robot_name'
	  8. , 'roman_numerals']
	- And here is the previous list:
	- 1. hello world
	  2. two-fer
	  3. reverse string
	  4. accumulate
	  5. series
	  6. robot_name
	  7. anagram
	  8. triangle
	  9. word_count
	  10. armstrong_numbers
	  11. difference_of_squares
	  12. roman_numerals
- uh... and there were 3 more too. alas, I forget. Well my documentation may be thorough but it's not perfect
- Got reverse-string working again. Also accumulate. I think I'll go ahead and merge this branch.
- I failed to realize that Clojure's `loop` macro could simply be used without destructuring! How did I not think of that? It's just a matter of replacing `(destructure bindings)` `bindings`:
- ```clojure
  (defmacro loop
    "Evaluates the exprs in a lexical context in which the symbols in
    the binding-forms are bound to their respective init-exprs or parts
    therein. Acts as a recur target."
    {:added "1.0", :special-form true, :forms '[(loop [bindings*] exprs*)]}
    [bindings & body]
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
- On the other hand, our `recur` is built into the interpreter, as a special form. I'm sure there's a way to make it work.
- Since there's no laziness, that's what is throwing up the biggest challenge right now. `interleave` has showed up a bunch.
- I could still implement them recursively, but would need to add a stop condition to prevent infinite loops.
- ```clojure
  (defn interleave
    "Returns a lazy seq of the first item in each coll, then the second etc."
    {:added "1.0"
     :static true}
    ([] ())
    ([c1] (lazy-seq c1))
    ([c1 c2]
       (lazy-seq
        (let [s1 (seq c1) s2 (seq c2)]
          (when (and s1 s2)
            (cons (first s1) (cons (first s2) 
                                   (interleave (rest s1) (rest s2))))))))
    ([c1 c2 & colls] 
       (lazy-seq 
        (let [ss (map seq (conj colls c2 c1))]
          (when (every? identity ss)
            (concat (map first ss) (apply interleave (map rest ss))))))))
  ```
- I just did the 2-arity, with a `loop`:
- ```clojure
  (defn interleave [c1 c2]
    (loop [s1  (seq c1)
           s2  (seq c2)
           res []]
      (if (or (empty? s1) (empty? s2))
        res
        (recur (rest s1) 
               (rest s2) 
               (cons (first s1) (cons (first s2) res))))))
  ```
- I just hope the other functions I want to implement don't use it in the other arities
- ![image.png](../assets/image_1690815314350_0.png){:height 427, :width 747}
- ```clojure
  (require "zip")
  
  (def tree 
    {:value 1, 
     :left {:value 2, 
            :left nil, 
            :right {:value 3, 
                    :left nil, 
                    :right nil}}, 
     :right {:value 4, 
             :left nil, 
             :right nil}})
  
  (-> tree
      zip/from-tree
      zip/left
      zip/right
      zip/value) => 3 
  ```
- Next: [[Clojure interpreter notes page 5]]