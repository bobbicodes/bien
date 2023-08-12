(defn sh [n [x & r]]
  (if x
    (if (sequential? x)
      (let [sub (sh n x)]
        (cons sub (sh (- n (reduce + (flatten sub))) r)))
      (if (<= x n)
        (cons x (sh (- n x) r))
        ()))))