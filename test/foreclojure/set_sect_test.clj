(deftest test-81
  (is (= (set-sect #{0 1 2 3} #{2 3 4 5}) #{2 3}))
  (is (= (set-sect #{0 1 2} #{3 4 5}) #{}))
  (is (= (set-sect #{:a :b :c :d} #{:c :e :a :f :d}) #{:a :c :d})))