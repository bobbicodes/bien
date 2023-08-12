(deftest test-28
  (is (= (makeflat '((1 2) 3 [4 [5 6]])) '(1 2 3 4 5 6)))
  (is (= (makeflat ["a" ["b"] "c"]) '("a" "b" "c")))
  (is (= (makeflat '((((:a))))) '(:a))))