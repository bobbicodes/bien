(defn my-merge-with [f & ms]
  (reduce (fn [am m]
            (into am (for [[k v] m]
                       (if (contains? am k)
                         [k (f (am k) v)]
                         [k v]))))
          ms))