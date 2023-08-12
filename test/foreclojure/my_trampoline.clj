(defn my-trampoline [f & x]
  (if (fn? f)
    (my-trampoline (apply f x))
    f))