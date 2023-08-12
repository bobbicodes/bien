(defn bal-num [n]
  (let [s (map #(- (int %) 48) (str n))
        l (/ (count s) 2)
        [a b] (map #(apply + (take l %)) [s (into () s)])]
    (= a b)))