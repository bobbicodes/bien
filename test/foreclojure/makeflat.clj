(defn makeflat [s]
  (filter (complement sequential?)
          (rest (tree-seq sequential? seq s))))