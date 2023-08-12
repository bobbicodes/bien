(defn pf [s]
  (if (every? coll? s)
    (mapcat pf s)
    [s]))