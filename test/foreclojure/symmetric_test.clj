(deftest test-96
  (is (= (symmetric? '(:a (:b nil nil) (:b nil nil))) true))
  (is (= (symmetric? '(:a (:b nil nil) nil)) false))
  (is (= (symmetric? '(:a (:b nil nil) (:c nil nil))) false))
  (is (= (symmetric? [1 [2 nil [3 [4 [5 nil nil] [6 nil nil]] nil]]
                      [2 [3 nil [4 [6 nil nil] [5 nil nil]]] nil]])
         true))
  (is (= (symmetric? [1 [2 nil [3 [4 [5 nil nil] [6 nil nil]] nil]]
                      [2 [3 nil [4 [5 nil nil] [6 nil nil]]] nil]])
         false))
  (is (= (symmetric? [1 [2 nil [3 [4 [5 nil nil] [6 nil nil]] nil]]
                      [2 [3 nil [4 [6 nil nil] nil]] nil]])
         false)))