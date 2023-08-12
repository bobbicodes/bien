(deftest test-140
  (is (= (veitch #{#{'a 'B 'C 'd}
                   #{'A 'b 'c 'd}
                   #{'A 'b 'c 'D}
                   #{'A 'b 'C 'd}
                   #{'A 'b 'C 'D}
                   #{'A 'B 'c 'd}
                   #{'A 'B 'c 'D}
                   #{'A 'B 'C 'd}})
         #{#{'A 'c}
           #{'A 'b}
           #{'B 'C 'd}}))
  (is (= (veitch #{#{'A 'B 'C 'D}
                   #{'A 'B 'C 'd}})
         #{#{'A 'B 'C}}))
  (is (= (veitch #{#{'a 'b 'c 'd}
                   #{'a 'B 'c 'd}
                   #{'a 'b 'c 'D}
                   #{'a 'B 'c 'D}
                   #{'A 'B 'C 'd}
                   #{'A 'B 'C 'D}
                   #{'A 'b 'C 'd}
                   #{'A 'b 'C 'D}})
         #{#{'a 'c}
           #{'A 'C}}))
  (is (= (veitch #{#{'a 'b 'c}
                   #{'a 'B 'c}
                   #{'a 'b 'C}
                   #{'a 'B 'C}})
         #{#{'a}}))
  (is (= (veitch #{#{'a 'B 'c 'd}
                   #{'A 'B 'c 'D}
                   #{'A 'b 'C 'D}
                   #{'a 'b 'c 'D}
                   #{'a 'B 'C 'D}
                   #{'A 'B 'C 'd}})
         #{#{'a 'B 'c 'd}
           #{'A 'B 'c 'D}
           #{'A 'b 'C 'D}
           #{'a 'b 'c 'D}
           #{'a 'B 'C 'D}
           #{'A 'B 'C 'd}}))
  (is (= (veitch #{#{'a 'b 'c 'd}
                   #{'a 'B 'c 'd}
                   #{'A 'B 'c 'd}
                   #{'a 'b 'c 'D}
                   #{'a 'B 'c 'D}
                   #{'A 'B 'c 'D}})
         #{#{'a 'c}
           #{'B 'c}}))
  (is (= (veitch #{#{'a 'B 'c 'd}
                   #{'A 'B 'c 'd}
                   #{'a 'b 'c 'D}
                   #{'a 'b 'C 'D}
                   #{'A 'b 'c 'D}
                   #{'A 'b 'C 'D}
                   #{'a 'B 'C 'd}
                   #{'A 'B 'C 'd}})
         #{#{'B 'd}
           #{'b 'D}}))
  (is (= (veitch #{#{'a 'b 'c 'd}
                   #{'A 'b 'c 'd}
                   #{'a 'B 'c 'D}
                   #{'A 'B 'c 'D}
                   #{'a 'B 'C 'D}
                   #{'A 'B 'C 'D}
                   #{'a 'b 'C 'd}
                   #{'A 'b 'C 'd}})
         #{#{'B 'D}
           #{'b 'd}}))
  (is (= (veitch #{#{'a 'b 'c 'd}
                   #{'A 'b 'c 'd}
                   #{'a 'B 'c 'D}
                   #{'A 'B 'c 'D}
                   #{'a 'B 'C 'D}
                   #{'A 'B 'C 'D}
                   #{'a 'b 'C 'd}
                   #{'A 'b 'C 'd}})
         #{#{'B 'D}
           #{'b 'd}})))