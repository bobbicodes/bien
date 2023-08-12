(deftest test-62
  (is (= (take 5 (spaz-out #(* 2 %) 1)) [1 2 4 8 16]))
  (is (= (take 100 (spaz-out inc 0)) (take 100 (range))))
  (is (= (take 9 (spaz-out #(inc (mod % 3)) 1)) (take 9 (cycle [1 2 3])))))