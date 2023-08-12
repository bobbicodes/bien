(deftest test-116
  (is (= false (ps 4)))
  (is (= true (ps 563)))
  (is (= 1103 (nth (filter ps (range)) 15))))