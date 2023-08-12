(defn mycomp [f & gs]
  (if gs
    #(f (apply (apply mycomp gs) (conj %& %)))
    f))