(deftest test-75
  (is (= (totient 1) 1))
  (is (= (totient 10) (count '(1 3 7 9)) 4))
  (is (= (totient 40) 16))
  (is (= (totient 99) 60)))