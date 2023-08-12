(deftest test-105
  (is (= {} (key-val [])))
  (is (= {:a [1]} (key-val [:a 1])))
  (is (= {:a [1], :b [2]} (key-val [:a 1, :b 2])))
  (is (= {:a [1 2 3], :b [], :c [4]} (key-val [:a 1 2 3 :b :c 4]))))