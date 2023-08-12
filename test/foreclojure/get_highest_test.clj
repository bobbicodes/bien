(deftest test-38
  (is (= (get-highest 1 8 3 4) 8))
  (is (= (get-highest 30 20) 30))
  (is (= (get-highest 45 67 11) 67)))