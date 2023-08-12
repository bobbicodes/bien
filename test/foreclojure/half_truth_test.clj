(deftest test-83
  (is (= false (half-truth false false)))
  (is (= true (half-truth true false)))
  (is (= false (half-truth true)))
  (is (= true (half-truth false true false)))
  (is (= false (half-truth true true true)))
  (is (= true (half-truth true true true false))))