(deftest test-84
  (is (let [divides #{[8 4] [9 3] [4 2] [27 9]}]
        (= (trans-closure divides) #{[4 2] [8 4] [8 2] [9 3] [27 9] [27 3]})))
  (is (let [more-legs
            #{["cat" "man"] ["man" "snake"] ["spider" "cat"]}]
        (= (trans-closure more-legs)
           #{["cat" "man"] ["cat" "snake"] ["man" "snake"]
             ["spider" "cat"] ["spider" "man"] ["spider" "snake"]})))
  (is (let [progeny
            #{["father" "son"] ["uncle" "cousin"] ["son" "grandson"]}]
        (= (trans-closure progeny)
           #{["father" "son"] ["father" "grandson"]
             ["uncle" "cousin"] ["son" "grandson"]}))))