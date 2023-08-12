(deftest test-40
  (is (= (inject 0 [1 2 3]) [1 0 2 0 3]))
  (is (= (apply str (inject ", " ["one" "two" "three"])) "one, two, three"))
  (is (= (inject :z [:a :b :c :d]) [:a :z :b :z :c :z :d])))