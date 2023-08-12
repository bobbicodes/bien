(deftest test-85
  (is (= (powerset #{1 :a}) #{#{1 :a} #{:a} #{} #{1}}))
  (is (= (powerset #{}) #{#{}}))
  (is (= (powerset #{1 2 3}) #{#{} #{1} #{2} #{3} #{1 2} #{1 3} #{2 3} #{1 2 3}}))
  (is (= (count (powerset (into #{} (range 10)))) 1024)))