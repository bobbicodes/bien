(defn eulerian [e]
  (if (#{0 2} (count (filter odd? (vals (frequencies (mapcat seq e))))))
    (not (next (reduce
                (fn [g e]
                  (let [[a b] (map (fn [n] (or (some #(if (% n) %) g) #{n})) e)]
                    (conj (disj g a b) (into a b))))
                #{}
                e)))
    false))