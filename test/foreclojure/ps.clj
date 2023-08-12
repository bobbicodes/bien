(defn ps [n]
  (first (for [o (range 1 (- n 2))
               [a b c] [(for [x [(- n o) (+ n o) n]]
                          (every? (fn [b] (> (rem x b) 0)) (range 2 x)))]
               :when (or a b)]
           (and a b c))))