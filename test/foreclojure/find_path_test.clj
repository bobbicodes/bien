(deftest test-106
  (is (= 1 (find-path 1 1)))
  (is (= 3 (find-path 3 12)))
  (is (= 3 (find-path 12 3)))
  (is (= 3 (find-path 5 9)))
  (is (= 9 (find-path 9 2)))
  (is (= 5 (find-path 9 12))))