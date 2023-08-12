(deftest test-118
  (is (= [3 4 5 6 7]
         (mymap inc [2 3 4 5 6])))
  (is (= (repeat 10 nil)
         (mymap (fn [_] nil) (range 10))))
  (is (= [1000000 1000001]
         (->> (mymap inc (range))
              (drop (dec 1000000))
              (take 2)))
      (= [1000000 1000001]
         (->> (mymap inc (range))
              (drop (dec 1000000))
              (take 2)))))