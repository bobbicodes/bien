(defn happy [n]
  (loop [n n
         s #{}]
    (let [x (apply + (map #(let [i (- (int %) (int \0))] (* i i)) (str n)))]
      (cond (= x 1) true
            (s x) false
            :else (recur x (conj s x))))))