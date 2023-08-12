(deftest test-110
  (is (= [[1 1] [2 1] [1 2 1 1]] (take 3 (seq-prons [1]))))
  (is (= [3 1 2 4] (first (seq-prons [1 1 1 4 4]))))
  (is (= [1 1 1 3 2 1 3 2 1 1] (nth (seq-prons [1]) 6)))
  (is (= 338 (count (nth (seq-prons [3 2]) 15)))))