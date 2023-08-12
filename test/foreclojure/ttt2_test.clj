(deftest test-119
  (is (= (ttt :x [[:o :e :e]
                  [:o :x :o]
                  [:x :x :e]])
         #{[2 2] [0 1] [0 2]}))
  (is (= (ttt :x [[:x :o :o]
                  [:x :x :e]
                  [:e :o :e]])
         #{[2 2] [1 2] [2 0]}))
  (is (= (ttt :x [[:x :e :x]
                  [:o :x :o]
                  [:e :o :e]])
         #{[2 2] [0 1] [2 0]}))
  (is (= (ttt :x [[:x :x :o]
                  [:e :e :e]
                  [:e :e :e]])
         #{}))
  (is (= (ttt :o [[:x :x :o]
                  [:o :e :o]
                  [:x :e :e]])
         #{[2 2] [1 1]})))