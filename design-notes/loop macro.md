- Time to burn my brain out on this some more.
- As usual, I feel like this should be very easy.
- Let's say we have a basic loop:
- ```clojure
  (loop [s [[1 2] [3 4]] res []]
      (if (empty? s) res
        (recur (rest s) (conj res (first s)))))
  ```
- Now let's say we have a trivial macro that just rewrites it as a `let`
- ```clojure
  (defmacro loop [bindings & body]
    `(let* ~bindings ~@(do body)))
  ```
- ```clojure
  (macroexpand
    (loop [s [[1 2] [3 4]] res []]
      (if (empty? s) res
        (recur (rest s) (conj res (first s))))))
  
  (let* [s [[1 2] [3 4]] res []]
    (if (empty? s) res
      (recur (rest s) (conj res (first s)))))
  ```
- And then, simply replace the recur with another call to `loop`, passing the values created by evaluating the `recur` forms. That is all. Thank you, please drive through
- So it would look like this:
- ```clojure
  (let* [s [[1 2] [3 4]] res []]
    (if (empty? s) res
      (loop [s (rest s) res (conj res (first s))]
        (if (empty? s) res
          (recur (rest s) (conj res (first s)))))))
  ```
- Maybe I could also have a `recur` macro that returns the second loop?? That might be more practical than trying to find it in the form. We would call `(recur (rest s) (conj res (first s)))` and it would return
- ```clojure
  (loop [s (rest s) res (conj res (first s))]
        (if (empty? s) res
          (recur (rest s) (conj res (first s)))))
  ```
- Seems exceedingly simple, doesn't it?
- So I guess we would need to pass the conditional part (`(if (empty? s) res ...)`) into the call to `recur` or something.
- This is the path to the `(recur ...)` form in this case:
- ```clojure
  (defmacro loop [bindings & body]
    (println (first (rest (rest (rest (first body))))))
    `(let* ~bindings ~@(do body)))
  
  ;; console output:
  (recur (rest s) (conj res (first s)))
  ```
- I suppose we could reliably find it by repeatedly calling `rest` until `(= recur (first x))`?
- Wait... won't it always be the last form? Well... what about cases where there's a conditional with multiple possible `recur`s? Not an edge case, this is like half the time.
- It could work if we find every recur form and insert the necessary args. But I'd better contrive an example like that.
- Oddly, ClojureDocs doesn't have an example like that.
- Oh wait, here's one:
- ```clojure
  ;; Trailing position could be multiple
  (loop [x 1]
    (println "x= " x)
    (cond
      (> x 10) (println "ending at " x )
      (even? x) (recur (* 2 x))
      :else (recur (+ x 1))))
  ```
- Also, remember we have the case where the target is a function, not a loop. Which means... we have to have some way of conveying the body and binding vars to it...
- What if we make them special forms like before, but just have a hash that is attached to the recur? That's what I just tried and I couldn't get it to work. repeatedly.
- How about we make a stack? It will be global, like before, but it will be a stack, and when we execute the recur, we pop the stack? Sounds great, actually!
- [[loop-stack]]
-