(defn my-reductions
  ([f [a & b]] (my-reductions f a b))
  ([f a b]
   (let [m (atom a)]
     (cons a (map #(swap! m f %) b)))))