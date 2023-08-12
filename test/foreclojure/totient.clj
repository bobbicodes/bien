(defn totient [a]
  (count
   (for [b (range a)
         :when (not-any? #(= 0 (rem a %) (rem b %)) (range 2 a))]
     b)))