(defn k [i s]
  (set
   (if (= i 0)
     [#{}]
     (mapcat #(for [p (k (- i 1) %2)] (conj p %))
             s (next (iterate next s))))))