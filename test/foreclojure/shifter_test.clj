(deftest test-44
  (is (= (shifter 2 [1 2 3 4 5]) '(3 4 5 1 2)))
  (is (= (shifter -2 [1 2 3 4 5]) '(4 5 1 2 3)))
  (is (= (shifter 6 [1 2 3 4 5]) '(2 3 4 5 1)))
  (is (= (shifter 1 '(:a :b :c)) '(:b :c :a)))
  (is (= (shifter -4 '(:a :b :c)) '(:c :a :b))))