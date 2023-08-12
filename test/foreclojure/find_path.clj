(defn find-path [s e]
  (loop [opts [s] depth 1]
    (if (some #{e} opts)
      depth
      (letfn [(solutions [n]
                (concat
                 [(* n 2) (+ n 2)]
                 (if (even? n) [(/ n 2)] [])))]
        (recur (mapcat solutions opts) (inc depth))))))