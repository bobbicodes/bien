(defn uce [x]
  (fn [m] ((fn e [x m]
             (if (seq? x)
               (apply ({'+ + '- - '* * '/ /} (first x))
                      (map #(e % m) (rest x)))
               (m x x)))
           x m)))