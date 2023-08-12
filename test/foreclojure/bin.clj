(defn bin [n]
  (apply + (map #(if (= \1 %1)
                   (apply * (repeat %2 2)) 0)
                (reverse n) (range))))