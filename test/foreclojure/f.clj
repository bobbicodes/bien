(defn f [grid]
  (let [neighbors (fn [[x y]] [[(inc x) y] [(dec x) y] [x (inc y)] [x (dec y)]])
        parts (for [y (range (count grid))
                    x (range (count (nth grid y)))
                    :let [e (get-in grid [y x])]]
                {({\C :cat \M :mouse \# :wall \space :space} e) [x y]})
        game (apply merge-with conj {:wall [] :space []} parts)
        spaces (conj (set (:space game)) (:mouse game))]
    (loop [open [(:cat game)] visited #{}]
      (cond
        (empty? open) false
        (= (first open) (:mouse game)) true
        :else (let [visited (conj visited (first open))
                    neigh (filter spaces (neighbors (first open)))
                    neigh (remove visited neigh)
                    open (concat (rest open) (remove visited neigh))]
                (recur open visited))))))