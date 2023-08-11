(ns core {:clj-kondo/ignore true})

(def not (fn [a] (if a false true)))

(defmacro cond
  (fn [& xs]
    (if (> (count xs) 0)
      (list 'if (first xs)
            (if (> (count xs) 1)
              (nth xs 1) (throw \\ "odd number of forms to cond \\"))
            (cons 'cond (rest (rest xs)))))))

(def dec (fn (a) (- a 1)))
(def zero? (fn (n) (= 0 n)))
(def identity (fn (x) x))

(defn _iter-> [acc form]
  (if (list? form)
    `(~(first form) ~acc ~@(rest form))
    (list form acc)))

(defmacro -> (fn [x & xs] (reduce _iter-> x xs)))

(defn _iter->> [acc form]
  (if (list? form)
    `(~(first form) ~@(rest form) ~acc) (list form acc)))

(defmacro ->> (fn (x & xs) (reduce _iter->> x xs)))