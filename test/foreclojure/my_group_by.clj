(defn my-group-by [f s]
  (reduce
   (fn [m x] (assoc m (f x) (conj (m (f x) []) x)))
   {} s))