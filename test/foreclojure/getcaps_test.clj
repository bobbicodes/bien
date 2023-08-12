(deftest test-29
  (is (= (getcaps "HeLlO, WoRlD!") "HLOWRD"))
  (is (empty? (getcaps "nothing")))
  (is (= (getcaps "$#A(*&987Zf") "AZ")))