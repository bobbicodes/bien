(defn fib [a b]
  (lazy-seq (cons a (fib b (+ a b)))))