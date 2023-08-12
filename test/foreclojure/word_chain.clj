(defn word-chain [s]
  (or (some (fn [w]
              ((fn f [a s]
                 (or (empty? s)
                     (some #(if (loop [[a & b :as c] (seq a) [d & e :as g] (seq %)]
                                  (if (= a d)
                                    (recur b e)
                                    (or (= b e) (= b g) (= c e))))
                              (f % (disj s %)))
                           s)))
               w (disj s w)))
            s)
      false))