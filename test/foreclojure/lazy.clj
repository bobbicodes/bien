(defn lazy [& s]
  (loop [v (vec s)]
    (let [first-vals (map first v)
          smallest (apply min first-vals)]
      (if (= smallest (apply max first-vals))
        smallest
        (let [i (apply min-key #(nth first-vals %) (range (count v)))]
          (recur (update-in v [i] next)))))))