(defn mymap [f l]
  (rest (reductions #(f %2) 0 l)))