(deftest test-61
  (is (= (make-map [:a :b :c] [1 2 3]) {:a 1, :b 2, :c 3}))
  (is (= (make-map [1 2 3 4] ["one" "two" "three"]) {1 "one", 2 "two", 3 "three"}))
  (is (= (make-map [:foo :bar] ["foo" "bar" "baz"]) {:foo "foo", :bar "bar"})))