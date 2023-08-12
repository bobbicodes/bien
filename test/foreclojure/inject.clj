(defn inject [x coll]
  (rest (interleave (repeat x) coll)))