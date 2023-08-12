(deftest test-32
  (is (= (dupseq [1 2 3]) '(1 1 2 2 3 3)))
  (is (= (dupseq [:a :a :b :b]) '(:a :a :a :a :b :b :b :b)))
  (is (= (dupseq [[1 2] [3 4]]) '([1 2] [1 2] [3 4] [3 4])))
  (is (= (dupseq [44 33]) [44 44 33 33])))