(defn graph [g]
  ((fn f [e]
     (#(if (= e %) (= % g) (f %))
      (reduce (fn [a b] (into a (filter #(some (set b) %) g)))
              #{}
              e)))
   #{(first g)}))