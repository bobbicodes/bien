(defn rn [n]
  (->> (map {\C 100 \D 500 \I 1 \L 50 \M 1000 \V 5 \X 10} n)
       (partition 2 1 [0])
       (map (fn [[a b]] (if (< a b) (- a) a)))
       (apply +)))