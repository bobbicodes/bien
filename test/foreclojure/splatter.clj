(defn splatter [s n]
  (mapcat
   (fn [s]
     (repeat n s)) s))