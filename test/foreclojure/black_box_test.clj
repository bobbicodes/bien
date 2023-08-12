(deftest test-65
  (is (= :map (black-box {:a 1, :b 2})))
  (is (= :list (black-box (range (rand-int 20)))))
  (is (= :set (black-box #{10 (rand-int 5)})))
  (is (= :vector (black-box [1 2 3 4 5 6])))
  (is (= [:map :set :vector :list] (map black-box [{} #{} [] ()]))))