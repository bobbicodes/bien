(defn count-elements [s]
  (loop [x s acc 0]
    (if (empty? x)
      acc
      (recur (rest x) (inc acc)))))