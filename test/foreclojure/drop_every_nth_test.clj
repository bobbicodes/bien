(deftest test-41
  (is (= (drop-every-nth [1 2 3 4 5 6 7 8] 3) [1 2 4 5 7 8]))
  (is (= (drop-every-nth [:a :b :c :d :e :f] 2) [:a :c :e]))
  (is (= (drop-every-nth [1 2 3 4 5 6] 4) [1 2 3 5 6])))