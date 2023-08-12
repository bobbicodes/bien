(defn ss [coll]
  (let [digits (fn digits [n]
                 (if (< n 10)
                   (list n)
                   (cons (rem n 10) (digits (quot n 10)))))]
    (count (filter
            #(< % (apply + (map * (digits %) (digits %))))
            coll))))