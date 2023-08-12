(defn tree? [n]
  (or (nil? n)
      (and (coll? n)
           (= 3 (count n))
           (every? tree? (rest n)))))