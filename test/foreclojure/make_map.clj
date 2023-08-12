(defn make-map [keys vals]
  (apply hash-map (interleave keys vals)))