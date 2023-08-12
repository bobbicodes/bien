(deftest test-53
  (is (= (longest-subseq [1 0 1 2 3 0 4 5]) [0 1 2 3]))
  (is (= (longest-subseq [5 6 1 3 2 7]) [5 6]))
  (is (= (longest-subseq [2 3 3 4 5]) [3 4 5]))
  (is (= (longest-subseq [7 6 5 4]) [])))