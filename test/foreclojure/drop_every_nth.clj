(defn drop-every-nth [s n]
  (apply concat (partition-all (dec n) n s)))