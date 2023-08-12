(deftest test-33
  (is (= (splatter [1 2 3] 2) '(1 1 2 2 3 3)))
  (is (= (splatter [:a :b] 4) '(:a :a :a :a :b :b :b :b)))
  (is (= (splatter [4 5 6] 1) '(4 5 6)))
  (is (= (splatter [[1 2] [3 4]] 2) '([1 2] [1 2] [3 4] [3 4])))
  (is (= (splatter [44 33] 2) [44 44 33 33])))