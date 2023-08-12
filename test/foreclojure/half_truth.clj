(defn half-truth [& vs]
  (true? (and (some not vs)
              (some identity vs))))