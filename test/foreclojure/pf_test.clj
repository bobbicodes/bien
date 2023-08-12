(deftest test-93
  (is (= (pf [["Do"] ["Nothing"]])
         [["Do"] ["Nothing"]]))
  (is (= (pf [[[[:a :b]]] [[:c :d]] [:e :f]])
         [[:a :b] [:c :d] [:e :f]]))
  (is (= (pf '((1 2) ((3 4) ((((5 6)))))))
         '((1 2) (3 4) (5 6)))))