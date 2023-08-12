(defn my-last [s]
  (if (next s)
    (recur (next s))
    (first s)))