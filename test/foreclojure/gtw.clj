(defn gtw [n p [x & r]]
  (lazy-seq
   (when x
     (let [remaining (if (p x) (dec n) n)]
       (when (pos? remaining)
         (cons x (gtw remaining p r)))))))