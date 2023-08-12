(deftest test-70
  (is (= (word-sort  "Have a nice day.")
         ["a" "day" "Have" "nice"]))
  (is (= (word-sort  "Clojure is a fun language!")
         ["a" "Clojure" "fun" "is" "language"]))
  (is (= (word-sort  "Fools fall for foolish follies.")
         ["fall" "follies" "foolish" "Fools" "for"])))