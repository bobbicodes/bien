(deftest test-46
  (is (= 3 ((flipper nth) 2 [1 2 3 4 5])))
  (is (= true ((flipper >) 7 8)))
  (is (= 4 ((flipper quot) 2 8)))
  (is (= [1 2 3] ((flipper take) [1 2 3 4 5] 3))))