(deftest test-80
  (is (= (perfect-nums 6) true))
  (is (= (perfect-nums 7) false))
  (is (= (perfect-nums 496) true))
  (is (= (perfect-nums 500) false))
  (is (= (perfect-nums 8128) true)))