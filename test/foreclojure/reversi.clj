(defn reversi [board p]
  (let [o '{b w w b}
        d (for [y [-1 0 1] x (if (= 0 y) [-1 1] [-1 0 1])] [y x])
        b (into {} (for [y (range 4) x (range 4)] [[y x] (get-in board [y x])]))
        e (map key (filter #(= 'e (val %)) b))
        wk (fn [st off] (take-while b (rest (iterate #(map + % off) st))))
        vl (fn [pth] (let [s (apply str (map b pth)) r (re-pattern (str (o p) "+" p))]
                       (if (re-seq r s) (take-while #(not= p (b %)) pth))))
        mv (fn [st] (set (apply concat (keep vl (map #(wk st %) d)))))]
    (into {} (for [st e :let [mvs (mv st)] :when (not-empty mvs)] [st mvs]))))