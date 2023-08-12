(defn classes [f c]
  (set (map set (vals (group-by f c)))))