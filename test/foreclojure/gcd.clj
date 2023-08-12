(defn gcd [a b]
  (if (= 0 b) a (recur b (mod a b))))