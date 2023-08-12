(deftest test-102
  (is (= (camel "something") "something"))
  (is (= (camel "multi-word-key") "multiWordKey"))
  (is (= (camel "leaveMeAlone") "leaveMeAlone")))