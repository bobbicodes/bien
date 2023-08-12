(deftest test-98
  (is (= (classes #(* % %) #{-2 -1 0 1 2})
         #{#{0} #{1 -1} #{2 -2}}))
  (is (= (classes #(rem % 3) #{0 1 2 3 4 5})
         #{#{0 3} #{1 4} #{2 5}}))
  (is (= (classes identity #{0 1 2 3 4})
         #{#{0} #{1} #{2} #{3} #{4}}))
  (is (= (classes (constantly true) #{0 1 2 3 4})
         #{#{0 1 2 3 4}})))