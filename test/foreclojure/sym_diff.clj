(defn sym-diff [a b]
  (into (set (remove b a)) (remove a b)))