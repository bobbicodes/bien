(deftest test-74
  (is (= (perfect-square "4,5,6,7,8,9") "4,9"))
  (is (= (perfect-square "15,16,25,36,37") "16,25,36")))