(defn compress [s]
  (map first
       (partition-by identity s)))