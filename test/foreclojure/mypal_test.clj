(deftest test-27
  (is (false? (mypal '(1 2 3 4 5))))
  (is (true? (mypal "racecar")))
  (is (true? (mypal [:foo :bar :foo])))
  (is (true? (mypal '(1 1 3 3 1 1))))
  (is (false? (mypal '(:a :b :c)))))