(deftest test-92
  (is (= 14 (rn "XIV")))
  (is (= 827 (rn "DCCCXXVII")))
  (is (= 3999 (rn "MMMCMXCIX")))
  (is (= 48 (rn "XLVIII"))))