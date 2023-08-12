(defn ttt [p board]
  (let [win? #(let [b (concat % (apply map list %)
                              [(map nth % (range)) (map nth (map reverse %) (range))])]
                (some #{[p p p]} b))]
    (set
     (for [y (range 3), x (range 3),
           :when (and (= :e (get-in board [y x]))
                      (win? (assoc-in board [y x] p)))]
       [y x]))))