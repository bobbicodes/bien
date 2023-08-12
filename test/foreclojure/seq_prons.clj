(defn seq-prons [s]
  (next (iterate #(mapcat (juxt count first)
                          (partition-by identity %)) s)))