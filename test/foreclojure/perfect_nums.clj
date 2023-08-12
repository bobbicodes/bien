(defn perfect-nums [x]
  (= x (apply + (filter #(= 0 (mod x %)) (range 1 x)))))