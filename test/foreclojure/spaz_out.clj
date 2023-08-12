(defn spaz-out [f init]
  (cons init
        (lazy-seq
         (spaz-out f (f init)))))