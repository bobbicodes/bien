(defn word-sort [s]
  (sort-by #(.toLowerCase %) (re-seq #"\w+" s)))