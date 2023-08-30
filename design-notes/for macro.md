- Ok so this is a good example of a binding form:
- ```clojure
  (to-groups '[x [1 2 3]
               y [1 2 3]
               :while (<= x y)
               z [1 2 3]])
  => [[x [1 2 3]]
      [y [1 2 3] [:while (<= x y)]] 
      [z [1 2 3]]]
  ```
- ## `emit-bind`
	- So let's take this one var at a time. We have the following function input:
	- ```clojure
	  [[[bind expr & mod-pairs]
	     & [[_ next-expr] :as next-groups]]]
	  ```
	- So our version, without the destructuring will be something like
	- ```clojure
	  (defn emit-bind [& next-groups]
	    )
	  ```
	- I can't make sense of the bindings... oh well, so then we have this:
	- ```clojure
	  (let [giter  (gensym "iter__")
	        gxs    (gensym "s__")
	        do-mod (fn do-mod [[[k v :as pair] & etc]]
	                 ...)
	  ```
	- oh wait, so we have this `do-mod` (modifiers?)
	- ```clojure
	  (fn do-mod [[[k v :as pair] & etc]]
	    (cond
	      (= k :let) `(let ~v ~(do-mod etc))
	      (= k :while) `(when ~v ~(do-mod etc))
	      (= k :when) `(if ~v
	                     ~(do-mod etc)
	                     (recur (rest ~gxs)))
	      next-groups
	      `(let [iterys# ~(emit-bind next-groups)
	             fs#     (seq (iterys# ~next-expr))]
	         (if fs#
	           (concat fs# (~giter (rest ~gxs)))
	           (recur (rest ~gxs))))
	      :else `(cons ~body-expr
	                   (~giter (rest ~gxs)))))
	  ```
	- I thought I'd make that its own function but it's actually tricky, because it also needs `next-groups`
- Maybe I'll ignore the modifier stuff for now, until the basic functionality is working.
- It seems that I can get rid of the chunked seq stuff and it still works. It's *much* simpler now:
- ```clojure
  (defmacro for* [seq-exprs body-expr]
    (let [to-groups (fn [seq-exprs]
                      (reduce (fn [groups [k v]]
                                (if (keyword? k)
                                  (conj (pop groups) (conj (peek groups) [k v]))
                                  (conj groups [k v])))
                              [] (partition 2 seq-exprs)))
          err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))
          emit-bind (fn emit-bind [[[bind expr & mod-pairs]
                                    & [[_ next-expr] :as next-groups]]]
                      (let [giter (gensym "iter__")
                            gxs (gensym "s__")
                            do-mod (fn do-mod [[[k v :as pair] & etc]]
                                     (cond
                                       (= k :let) `(let ~v ~(do-mod etc))
                                       (= k :while) `(when ~v ~(do-mod etc))
                                       (= k :when) `(if ~v
                                                      ~(do-mod etc)
                                                      (recur (rest ~gxs)))
                                       (keyword? k) (err "Invalid 'for' keyword " k)
                                       next-groups
                                       `(let [iterys# ~(emit-bind next-groups)
                                              fs# (seq (iterys# ~next-expr))]
                                          (if fs#
                                            (concat fs# (~giter (rest ~gxs)))
                                            (recur (rest ~gxs))))
                                       :else `(cons ~body-expr
                                                    (~giter (rest ~gxs)))))]
                        (if next-groups
                          `(fn ~giter [~gxs]
                             (lazy-seq
                              (loop [~gxs ~gxs]
                                (when-first [~bind ~gxs]
                                  ~(do-mod mod-pairs)))))
                          (let [gi (gensym "i__")
                                gb (gensym "b__")]
                            `(fn ~giter [~gxs]
                               (loop [~gxs ~gxs]
                                 (when-let [~gxs (seq ~gxs)]
                                   (let [~bind (first ~gxs)]
                                     ~(do-mod mod-pairs)))))))))]
     `(let [iter# ~(emit-bind (to-groups seq-exprs))]
       (iter# ~(second seq-exprs)))))
  ```
- Could be all that's left is to remove the destructuring.
- Here's the `emit-bind` arglist, halfway de-destructured, which still works with the appropriate modification: `[[[bind expr & mod-pairs] & next-groups]]`
- I see, the `to-groups` fn separates the bindings into bind-exprs and mod-pairs, like this:
- `[x [0 1 2 3 4 5] [:let [y (* x 3)]] [:when (even? y)]]`
- Ah. So each binding in bindings has the bind+expr *followed by* the modifiers. That's from this part:
- ```clojure
  (if (keyword? k)
      (conj (pop groups) (conj (peek groups) [k v]))
      (conj groups [k v])))
  ```
- So we can extract mod-pairs by `(subvec (first bindings) 2)`
- I did it!  Now it's (almost) destructuring-free:
- ```clojure
  (defmacro for* [seq-exprs body-expr]
    (let [to-groups (fn [seq-exprs]
                      (reduce (fn [groups [k v]]
                                (if (keyword? k)
                                  (conj (pop groups) (conj (peek groups) [k v]))
                                  (conj groups [k v])))
                              [] (partition 2 seq-exprs)))
          err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))
          emit-bind (fn emit-bind [bindings]
                      ;(println "mod-pairs" mod-pairs)
                      (println "mod-pairs" (subvec (first bindings) 2))
                      (println "ffirst of bindings:" (ffirst bindings))
                      (println "bindings:" bindings)
                      (println "next-groups:" (next bindings))
                      (let [giter (gensym "iter__")
                            gxs (gensym "s__")
                            do-mod (fn do-mod [[[k v :as pair] & etc]]
                                     (cond
                                       (= k :let) `(let ~v ~(do-mod etc))
                                       (= k :while) `(when ~v ~(do-mod etc))
                                       (= k :when) `(if ~v
                                                      ~(do-mod etc)
                                                      (recur (rest ~gxs)))
                                       (keyword? k) (err "Invalid 'for' keyword " k)
                                       (next bindings)
                                       `(let [iterys# ~(emit-bind (next bindings))
                                              fs# (seq (iterys# ~(second (first (next bindings)))))]
                                          (if fs#
                                            (concat fs# (~giter (rest ~gxs)))
                                            (recur (rest ~gxs))))
                                       :else `(cons ~body-expr
                                                    (~giter (rest ~gxs)))))]
                        (if (next bindings)
                          `(fn ~giter [~gxs]
                             (lazy-seq
                              (loop [~gxs ~gxs]
                                (when-first [~(ffirst bindings) ~gxs]
                                  ~(do-mod (subvec (first bindings) 2))))))
                          (let [gi (gensym "i__")
                                gb (gensym "b__")]
                            `(fn ~giter [~gxs]
                               (loop [~gxs ~gxs]
                                 (when-let [~gxs (seq ~gxs)]
                                   (let [~(ffirst bindings) (first ~gxs)]
                                     ~(do-mod (subvec (first bindings) 2))))))))))]
     `(let [iter# ~(emit-bind (to-groups seq-exprs))]
       (iter# ~(second seq-exprs)))))
  ```
- Done!
- ```clojure
  (defmacro for* [seq-exprs body-expr]
    (let [to-groups (fn [seq-exprs]
                      (reduce (fn [groups [k v]]
                                (if (keyword? k)
                                  (conj (pop groups) (conj (peek groups) [k v]))
                                  (conj groups [k v])))
                              [] (partition 2 seq-exprs)))
          err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))
          emit-bind (fn emit-bind [bindings]
                      (let [giter (gensym "iter__")
                            gxs (gensym "s__")
                            do-mod (fn do-mod [domod]
                                     (cond
                                       (= (ffirst domod) :let) `(let ~(second (first domod)) ~(do-mod (next domod)))
                                       (= (ffirst domod) :while) `(when ~(second (first domod)) ~(do-mod (next domod)))
                                       (= (ffirst domod) :when) `(if ~(second (first domod))
                                                      ~(do-mod (next domod))
                                                      (recur (rest ~gxs)))
                                       (keyword? (ffirst domod)) (err "Invalid 'for' keyword " (ffirst domod))
                                       (next bindings)
                                       `(let [iterys# ~(emit-bind (next bindings))
                                              fs# (seq (iterys# ~(second (first (next bindings)))))]
                                          (if fs#
                                            (concat fs# (~giter (rest ~gxs)))
                                            (recur (rest ~gxs))))
                                       :else `(cons ~body-expr
                                                    (~giter (rest ~gxs)))))]
                        (if (next bindings)
                          `(fn ~giter [~gxs]
                             (lazy-seq
                              (loop [~gxs ~gxs]
                                (when-first [~(ffirst bindings) ~gxs]
                                  ~(do-mod (subvec (first bindings) 2))))))
                            `(fn ~giter [~gxs]
                               (loop [~gxs ~gxs]
                                 (when-let [~gxs (seq ~gxs)]
                                   (let [~(ffirst bindings) (first ~gxs)]
                                     ~(do-mod (subvec (first bindings) 2)))))))))]
     `(let [iter# ~(emit-bind (to-groups seq-exprs))]
       (iter# ~(second seq-exprs)))))
  ```
- I think it's almost there. But it isn't working because we don't have named lambdas. Maybe I can define them as helper functions.
- There's like a dozen reasons why it might not work. It's like I spent all night performing surgery and it's all in shambles. I'm not upset though... it still feels like I made a lot of progress, like I have some idea how it works so I can imagine it working eventually.
- Trying to deconstruct this macro and realizing that I have no idea how macros work...
- Here's what I've got, with the helper fns separated. It works in Clojure:
- ```clojure
  (defn emit-bind [bindings body-expr]
    (println "calling emit-bind")
        (let [giter (gensym)
              gxs (gensym)]
          (if (next bindings)
            `(defn ~giter [~gxs]
               (loop [~gxs ~gxs]
                 (when-first [~(ffirst bindings) ~gxs]
                   ~(do-mod (subvec (first bindings) 2) body-expr bindings giter gxs))))
            `(defn ~giter [~gxs]
               (loop [~gxs ~gxs]
                 (when-let [~gxs (seq ~gxs)]
                   (let [~(ffirst bindings) (first ~gxs)]
                     ~(do-mod (subvec (first bindings) 2) body-expr bindings giter gxs))))))))
  
  (defn do-mod [domod body-expr bindings giter gxs]
    (cond
      (= (ffirst domod) :let)
      `(let ~(second (first domod)) ~(do-mod (next domod) body-expr bindings giter gxs))
      (= (ffirst domod) :while)
      `(when ~(second (first domod)) ~(do-mod (next domod) body-expr bindings giter gxs))
      (= (ffirst domod) :when)
      `(if ~(second (first domod))
         ~(do-mod (next domod) body-expr bindings giter gxs)
         (recur (rest ~gxs) body-expr bindings giter gxs))
      (keyword? (ffirst domod)) (throw (str "Invalid 'for' keyword " (ffirst domod)))
      (next bindings)
      (let [iterys# (gensym)
            fs#     (gensym)]
        `(let [iterys# ~(emit-bind (next bindings) body-expr)
               fs#     (seq (iterys# ~(second (first (next bindings)))))]
           (if fs#
             (concat fs# (~giter (rest ~gxs)))
             (recur (rest ~gxs) body-expr bindings giter gxs))))
      :else `(cons ~body-expr
                   (~giter (rest ~gxs)))))
  
  (defmacro for [seq-exprs body-expr]
    (let [to-groups
          (fn [seq-exprs]
            (reduce (fn [groups kv]
                      (if (keyword? (first kv))
                        (conj (pop groups) (conj (peek groups) [(first kv) (last kv)]))
                        (conj groups [(first kv) (last kv)])))
                    [] (partition 2 seq-exprs)))
          iter# (gensym)]
      `(let [iter# ~(emit-bind (to-groups seq-exprs) body-expr)]
         (iter# ~(second seq-exprs)))))
  ```
- I'm tempted to simplify it by getting rid of the modifier stuff.
- It almost works! I think it might be having problems due to there being no auto-gensym, so perhaps I'll remedy that now.
- `when-first` doesn't work. But `when-let` does.
- Here's the `and` macro, which shows how to use `gensym`:
- ```clojure
  (defmacro and [& xs]
    (cond (empty? xs)      true
          (= 1 (count xs)) (first xs)
          true
          (let [condvar (gensym)]
            `(let [~condvar ~(first xs)]
               (if ~condvar (and ~@(rest xs)) ~condvar)))))
  ```
- Cool, I fixed `if-let`, `when-let` and `when-first`. Now, the de-modifier `for` almost works:
- ```clojure
  (for* [x [0 1 2 3 4 5]
         y [0 1 2 3 4 5]]
  (str x y))
  => ("00" "01" "02" "03" "04" "05" nil "10" "11" "12" "13" "14" "15" nil "20" "21" "22" "23" "24" "25" nil "30" "31" "32" "33" "34" "35" nil "40" "41" "42" "43" "44" "45" nil "50" "51" "52" "53" "54" "55" nil nil)
  ```
- The `nil`s are probably there because of something left over from the modifier stuff.
- I could patch it up temporarily by just removing the nils.
- It works, and now there are 84 tests passing. A few more than before. Nice!
- The modifiers are failing because of a problem with `first` (and by extension `ffirst`).
- This works:
- ```clojure
  (for [x (range 1 6)
        :let [y (* x x)
              z (* x x x)]]
    [x y z])
  => ([1 1 1] [2 4 8] [3 9 27] [4 16 64] [5 25 125] nil)
  ```
- Wow! I wasn't sure this moment would ever come!
- This causes an infinite loop:
- ```clojure
  (for [x [0 1 2 3 4 5]
      :let [y (* x 3)]
      :when (even? y)]
  y)
  ```
- So does this:
- ```clojure
  (for [x (range 3) y (range 3) :when (not= x y)] [x y])
  ```
- Actually that wouldn't work anyway because destructuring. Perhaps that will be the next thing I'll tackle
- This works:
- ```clojure
  (defn prime? [n]
           (not-any? zero? (map #(rem n %) (range 2 n))))
  
  (for [x (range 3 33 2) :while (prime? x)]
           x)
  => (3 5 7 nil)
  ```
- So it appears that `let` and `while` work, but `when` sometimes causes a cycle, and other times fails like so:
- ```clojure
  (defn prime? [n]
           (not-any? zero? (map #(rem n %) (range 2 n))))
  
  (for [x (range 3 33 2) :when (prime? x)]
           x)
  => 
  Error: 'G__36' not found
  ```
- I fixed the infinite loop problem.
- Hmm this fails although it uses `while`
- ```clojure
  (for [x [1 2 3]
        y [1 2 3]
        :while (<= x y)
        z [1 2 3]]
      [x y z])
  => 
  Error: 'body-expr' not found
  ```
- `macroexpand` might help here:
- ```clojure
  (macroexpand
   '(for [x [0 1 2 3 4 5]
         y [0 1 2 3 4 5]]
     (str x y)))
  
  (let [iter__1 
        (fn [s__3] 
          (loop [s__3 s__3] 
            (when-first [x s__3] 
              (let [iterys__6 
                    (fn [s__10] 
                      (loop [s__10 s__10] 
                        (when-let [s__10 (seq s__10)] 
                          (let [y (first s__10)] 
                            (cons (str x y) (iter__9 (rest s__10))))))) 
                    fs__7 (seq (iterys__6 [0 1 2 3 4 5]))] 
                (if fs__7 (concat fs__7 (iter__2 (rest s__3))) 
                    (recur 
                     (rest s__3) 
                     (str x y) 
                     [[x [0 1 2 3 4 5]] [y [0 1 2 3 4 5]]] 
                     iter__2 s__3))))))] 
    (iter__1 [0 1 2 3 4 5]))
  ```
- I can't believe I didn't try this earlier!
- I made named `gensym`s which will make it easier to debug.
- Here's the macroexpansion of the one which works, with the `defn` forms:
- ```clojure
  (macroexpand
   (for [x [0 1 2 3 4 5]
         y [0 1 2 3 4 5]]
     (str x y)))
  
  (let [iter__1 
        (defn iter__2 [s__3] 
          (loop [s__3 s__3] 
            (when-first [x s__3] 
              (let [iterys__6 
                    (defn iter__9 [s__10] 
                      (loop [s__10 s__10] 
                        (when-let [s__10 (seq s__10)] 
                          (let [y (first s__10)] 
                            (cons (str x y) (iter__9 (rest s__10))))))) 
                    fs__7 (seq (iterys__6 [0 1 2 3 4 5]))] 
                (if fs__7 (concat fs__7 (iter__2 (rest s__3))) 
                    (recur (rest s__3) (str x y) [[x [0 1 2 3 4 5]] [y [0 1 2 3 4 5]]] iter__2 s__3))))))] 
    (iter__1 [0 1 2 3 4 5]))
  ```
- We can see why it works, because the `iter__9` function is defined so it can be called later.
- Now let's look at one which doesn't work
- ```clojure
  (macroexpand
   (for [x [0 1 2 3 4 5]
         :let [y (* x 3)]
         :when (even? y)]
     y))
  
  (let [iter__14 
        (defn iter__15 [s__16] 
          (loop [s__16 s__16] 
            (when-let [s__16 (seq s__16)] 
              (let [x (first s__16)] 
                (let [y (* x 3)] 
                  (if (even? y) (cons y (iter__15 (rest s__16))) 
                      (recur (rest s__16) y 
                             [[x [0 1 2 3 4 5] 
                               [:let [y (* x 3)]] 
                               [:when (even? y)]]] 
                             iter__15 s__16)))))))] 
    (iter__14 [0 1 2 3 4 5]))
  ```
- It fails with `Error: 'x' not found`
- I started over, this time keeping it all in one function.
- But now `body-expr` is returning `nil` for some reason
- ```clojure
  (macroexpand
   (for [x [0 1 2 3 4 5]
         y [0 1 2 3 4 5]]
     (str x y)))
  
  (let [G__1 
        (defn iter__2 [s__3] 
          (loop [s__3 s__3] 
            (when-first [x s__3] 
              (let [iterys__4 
                    (defn iter__9 [s__10] 
                      (loop [s__10 s__10] 
                        (when-let [s__10 (seq s__10)] 
                          (let [y (first s__10)] 
                            (cons nil (iter__9 (rest s__10))))))) 
                    fs__5 (seq (iterys__4 [0 1 2 3 4 5]))] 
                (if fs__5 (concat fs__5 (iter__2 (rest s__3)))
                    (recur (rest s__3)))))))] 
    (G__1 [0 1 2 3 4 5]))
  ```
- Ah! I figured it out!
- But this whole thing didn't solve the problem... and we're back to `:when` causing an infinite loop. If we expand it, it's obvious why:
- ```clojure
  (macroexpand
   (for [x [0 1 2 3 4 5]
         :let [y (* x 3)]
         :when (even? y)]
     y))
  
  (let [G__1 
        (defn iter__2 [s__3] 
          (loop [s__3 s__3] 
            (when-let [s__3 (seq s__3)] 
              (let [x (first s__3)] 
                (let [y (* x 3)] 
                  (if (even? y) (cons y (iter__2 (rest s__3))) 
                      (recur (rest s__3))))))))] 
    (G__1 [0 1 2 3 4 5]))
  ```
- It's recurring with the same value as before! Oh wait... no it's not. This code should work. In fact, it works if I execute it in Clojure! So that means I've got a problem with `loop` or something...
- This one also fails, but works in Clojure:
- ```clojure
  (macroexpand
   (for [x [1 2 3]
         y [1 2 3]
         :while (<= x y)
         z [1 2 3]]
     [x y z]))
  
  (let [G__53 
        (defn iter__54 [s__55] 
          (loop [s__55 s__55] 
            (when-first [x s__55] 
              (let [iterys__56 
                    (defn iter__61 [s__62] 
                      (loop [s__62 s__62] 
                        (when-first [y s__62] 
                          (when (<= x y) 
                            (let [iterys__63 
                                  (defn iter__69 [s__70]
                                    (loop [s__70 s__70]
                                      (when-let [s__70 (seq s__70)]
                                        (let [z (first s__70)] 
                                          (cons [x y z] (iter__69 (rest s__70)))))))
                                  fs__64 (seq (iterys__63 [1 2 3]))] 
                              (if fs__64 (concat fs__64 (iter__61 (rest s__62))) 
                                  (recur (rest s__62))))))))
                    fs__57 (seq (iterys__56 [1 2 3]))] 
                (if fs__57 (concat fs__57 (iter__54 (rest s__55)))
                    (recur (rest s__55)))))))] (G__53 [1 2 3]))
  ```
- It fails because it can't find `s__62`.