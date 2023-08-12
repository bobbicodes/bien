(defn longest-subseq [s]
  (or (first (for [l (reverse (range 2 (count s)))
                   f (filter #(apply < %) (partition l 1 s))]
               f)) []))