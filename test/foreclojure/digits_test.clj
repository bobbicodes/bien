(deftest test-99
  (is (= (digits 1 1) [1]))
  (is (= (digits 99 9) [8 9 1]))
  (is (= (digits 999 99) [9 8 9 0 1])))