(deftest test-108
  (is (= 3 (lazy [3 4 5])))
  (is (= 4 (lazy [1 2 3 4 5 6 7] [0.5 3/2 4 19])))
  (is (= 64 (lazy (map #(* % % %) (range))
                  (filter #(zero? (bit-and % (dec %))) (range))
                  (iterate inc 20))))
  (is (= 7 (lazy (range) (range 0 100 7/6) [2 3 5 7 11 13]))))