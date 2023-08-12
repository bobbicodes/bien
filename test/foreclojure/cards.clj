(defn cards [c]
  {:suit ({\H :heart \C :club \S :spade \D :diamond} (first c))
   :rank ((zipmap "23456789TJQKA" (range)) (second c))})