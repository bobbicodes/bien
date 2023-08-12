(deftest test-86
  (is (= (happy 7) true))
  (is (= (happy 986543210) true))
  (is (= (happy 2) false))
  (is (= (happy 3) false)))