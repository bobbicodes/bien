(deftest test-107
  (is (= 256 ((closure 2) 16), ((closure 8) 2)))
  (is (= [1 8 27 64] (map (closure 3) [1 2 3 4])))
  (is (= [1 2 4 8 16] (map #((closure %) 2) [0 1 2 3 4]))))