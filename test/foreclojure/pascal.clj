(defn pascal [n]
  (loop [x 1 l [1]]
    (if (= x n) l
        (recur (inc x) (vec (map + (cons 0 l) (conj l 0)))))))