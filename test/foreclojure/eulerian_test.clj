(deftest test-89
  (is (= true (eulerian [[:a :b]])))
  (is (= false (eulerian [[:a :a] [:b :b]])))
  (is (= false (eulerian [[:a :b] [:a :b] [:a :c] [:c :a]
                          [:a :d] [:b :d] [:c :d]])))
  (is (= true (eulerian [[1 2] [2 3] [3 4] [4 1]])))
  (is (= true (eulerian [[:a :b] [:a :c] [:c :b] [:a :e]
                         [:b :e] [:a :d] [:b :d] [:c :e]
                         [:d :e] [:c :f] [:d :f]])))
  (is (= false (eulerian [[1 2] [2 3] [2 4] [2 5]]))))