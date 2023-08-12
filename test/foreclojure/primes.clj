(defn primes [n]
  (->>
   (range)
   (drop 2)
   (filter (fn [x] (every? #(< 0 (mod x %)) (range 2 x))))
   (take n)))