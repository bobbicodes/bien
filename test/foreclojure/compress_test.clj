(deftest test-30
  (is (= (apply str (compress "Leeeeeerrroyyy")) "Leroy"))
  (is (= (compress [1 1 2 3 3 2 2 3]) '(1 2 3 2 3)))
  (is (= (compress [[1 2] [1 2] [3 4] [1 2]]) '([1 2] [3 4] [1 2]))))