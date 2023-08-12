(deftest test-82
  (is (= true (word-chain #{"hat" "coat" "dog" "cat" "oat" "cot" "hot" "hog"})))
  (is (= false (word-chain #{"cot" "hot" "bat" "fat"})))
  (is (= false (word-chain #{"to" "top" "stop" "tops" "toss"})))
  (is (= true (word-chain #{"spout" "do" "pot" "pout" "spot" "dot"})))
  (is (= true (word-chain #{"share" "hares" "shares" "hare" "are"})))
  (is (= false (word-chain #{"share" "hares" "hare" "are"}))))