(deftest test-120
  (is (= 8 (ss (range 10))))
  (is (= 19 (ss (range 30))))
  (is (= 50 (ss (range 100))))
  (is (= 50 (ss (range 1000)))))