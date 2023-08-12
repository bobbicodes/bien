(deftest test-91
  (is (= true (graph #{[:a :a]})))
  (is (= true (graph #{[:a :b]})))
  (is (= false (graph #{[1 2] [2 3] [3 1]
                        [4 5] [5 6] [6 4]})))
  (is (= true (graph #{[1 2] [2 3] [3 1]
                       [4 5] [5 6] [6 4] [3 4]})))
  (is (= false (graph #{[:a :b] [:b :c] [:c :d]
                        [:x :y] [:d :a] [:b :e]})))
  (is (= true (graph #{[:a :b] [:b :c] [:c :d]
                       [:x :y] [:d :a] [:b :e] [:x :a]}))))