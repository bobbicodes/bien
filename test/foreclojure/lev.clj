(defn lev [s t]
  (let [f (fn [f s t]
            (cond
              (empty? s) (count t)
              (empty? t) (count s)
              :else (let [cost (if (= (first s) (first t)) 0 1)]
                      (min (inc (f f (rest s) t))
                           (inc (f f s (rest t)))
                           (+ cost (f f (rest s) (rest t)))))))
        g (memoize f)]
    (g g s t)))