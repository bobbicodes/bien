(defn powerset [s]
  (reduce #(into % (for [subset %] (conj subset %2))) #{#{}} s))