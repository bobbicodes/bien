(deftest test-121
  (is (= 2 ((uce '(/ a b))
            '{b 8 a 16})))
  (is (= 8 ((uce '(+ a b 2))
            '{a 2 b 4})))
  (is (= [6 0 -4]
         (map (uce '(* (+ 2 a)
                       (- 10 b)))
              '[{a 1 b 8}
                {b 5 a -2}
                {a 2 b 11}])))
  (is (= 1 ((uce '(/ (+ x 2)
                     (* 3 (+ y 1))))
            '{x 4 y 1}))))