(defn lcm [& args]
  (let [gcd (fn [a b] (if (zero? b) a (recur b (mod a b))))]
    (/ (reduce * args) (reduce gcd args))))