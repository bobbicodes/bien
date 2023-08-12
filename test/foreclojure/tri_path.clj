(defn tri-path [s]
  (first
   (reduce
    #(map + (map min (butlast %1) (rest %1)) %2)
    (reverse s))))