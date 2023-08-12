(deftest test-73
  (is (= nil (ttt [[:e :e :e] [:e :e :e] [:e :e :e]])))
  (is (= :x (ttt [[:x :e :o] [:x :e :e] [:x :e :o]])))
  (is (= :o (ttt [[:e :x :e] [:o :o :o] [:x :e :x]])))
  (is (= nil (ttt [[:x :e :o] [:x :x :e] [:o :x :o]])))
  (is (= :x (ttt [[:x :e :e] [:o :x :e] [:o :e :x]])))
  (is (= :o (ttt [[:x :e :o] [:x :o :e] [:o :e :x]])))
  (is (= nil (ttt [[:x :o :x] [:x :o :x] [:o :x :o]]))))