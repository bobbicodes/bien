(deftest test-125
  (is (= (str '(fn [x] (str x x))
              '(fn [x] (str x x)))
         ((fn [x] (str x x))
          '(fn [x] (str x x))))))