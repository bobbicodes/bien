(defn getcaps [s]
  (apply str (re-seq #"[A-Z]" s)))