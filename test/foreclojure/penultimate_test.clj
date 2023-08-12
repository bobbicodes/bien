(deftest test-20
  (is (= (penultimate (list 1 2 3 4 5)) 4))
  (is (= (penultimate ["a" "b" "c"]) "b"))
  (is (= (penultimate [[1 2] [3 4]]) [1 2])))