(deftest test-111
  (is (= true  (cw "the" ["_ # _ _ e"])))
  (is (= false (cw "the" ["c _ _ _"
                          "d _ # e"
                          "r y _ _"])))
  (is (= true  (cw "joy" ["c _ _ _"
                          "d _ # e"
                          "r y _ _"])))
  (is (= false (cw "joy" ["c o n j"
                          "_ _ y _"
                          "r _ _ #"])))
  (is (= true  (cw "clojure" ["_ _ _ # j o y"
                              "_ _ o _ _ _ _"
                              "_ _ f _ # _ _"]))))