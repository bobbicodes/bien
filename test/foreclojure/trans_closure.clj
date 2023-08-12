(defn trans-closure [r]
  (loop [s r]
    (let [n (into s
                  (for [[a b] s [c d] s
                        :when (= b c)]
                    [a d]))]
      (if (= n s) n (recur n)))))