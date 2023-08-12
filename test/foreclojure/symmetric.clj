(defn symmetric? [t]
  (= t ((fn m [[v l r]] (if v [v (m r) (m l)])) t)))