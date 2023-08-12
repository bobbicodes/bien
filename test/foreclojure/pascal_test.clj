(deftest test-97
  (is (= (pascal 1) [1]))
  (is (= (map pascal (range 1 6))
         [[1]
          [1 1]
          [1 2 1]
          [1 3 3 1]
          [1 4 6 4 1]]))
  (is (= (pascal 11)
         [1 10 45 120 210 252 210 120 45 10 1])))