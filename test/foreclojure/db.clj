(defn digits [n b]
  (if (< n b)
    [n]
    (conj (digits (quot n b) b) (rem n b))))