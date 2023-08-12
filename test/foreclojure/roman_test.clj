(deftest test-104
  (is (= "I" (roman 1)))
  (is (= "I" (roman 1)))
  (is (= "XXX" (roman 30)))
  (is (= "IV" (roman 4)))
  (is (= "CXL" (roman 140)))
  (is (= "DCCCXXVII" (roman 827)))
  (is (= "MMMCMXCIX" (roman 3999)))
  (is (= "XLVIII" (roman 48))))