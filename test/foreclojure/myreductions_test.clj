(deftest test-60
  (is (= (take 5 (my-reductions + (range))) [0 1 3 6 10]))
  (is (= (my-reductions conj [1] [2 3 4]) [[1] [1 2] [1 2 3] [1 2 3 4]]))
  (is (= (last (my-reductions * 2 [3 4 5])) (reduce * 2 [3 4 5]) 120)))