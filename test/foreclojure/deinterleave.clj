(defn deinterleave [coll n]
  (apply map list (partition n coll)))