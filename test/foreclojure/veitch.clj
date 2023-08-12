(defn veitch [I]
  (disj (into #{} (map (fn [s]
                         ((reduce
                           (fn [[d f u] x]
                             (let [U (disj u x)
                                   m (fn [t] (map #(conj % t) f))
                                   P #(symbol (.toUpperCase (str %)))
                                   L #(symbol (.toLowerCase (str %)))
                                   F (into (m (L x)) (m (P x)))]
                               (if (every? #(contains? I %) (map #(into (into d U) %) F))
                                 [d F U]
                                 [(conj d x) f U])))
                           [#{} [#{}] s]
                           s) 0)) I))
        '#{A d}))