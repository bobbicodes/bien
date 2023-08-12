(defn intervals [coll]
  (reverse (reduce (fn [[[a b] & r :as is] n]
                     (if (and a (= (inc b) n))
                       (cons [a n] r)
                       (cons [n n] is)))
                   ()
                   (distinct (sort coll)))))