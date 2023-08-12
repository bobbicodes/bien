(deftest test-34
  (is (= (myrange 1 4) '(1 2 3)))
  (is (= (myrange -2 2) '(-2 -1 0 1)))
  (is (= (myrange 5 8) '(5 6 7))))