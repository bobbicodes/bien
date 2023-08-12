(deftest test-114
  (is (= [2 3 5 7 11 13]
         (gtw 4 #(= 2 (mod % 3))
              [2 3 5 7 11 13 17 19 23])))
  (is (= ["this" "is" "a" "sentence"]
         (gtw 3 #(some #{\i} %)
              ["this" "is" "a" "sentence" "i" "wrote"])))
  (is (= ["this" "is"]
         (gtw 1 #{"a"}
              ["this" "is" "a" "sentence" "i" "wrote"]))))