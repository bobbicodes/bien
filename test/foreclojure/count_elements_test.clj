(deftest test-22
  (is (= (count-elements '(1 2 3 3 1)) 5))
  (is (= (count-elements "Hello World") 11))
  (is (= (count-elements [[1 2] [3 4] [5 6]]) 3))
  (is (= (count-elements '(13)) 1))
  (is (= (count-elements '(:a :b :c)) 3)))