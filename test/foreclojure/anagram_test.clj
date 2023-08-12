(deftest test-77
  (is (= (anagram ["meat" "mat" "team" "mate" "eat"]) #{#{"meat" "team" "mate"}}))
  (is (= (anagram ["veer" "lake" "item" "kale" "mite" "ever"]) #{#{"veer" "ever"} #{"lake" "kale"} #{"mite" "item"}})))