(ns core {:clj-kondo/ignore true})

(def not (fn [a] (if a false true)))

(defmacro cond
  (fn [& xs]
    (if (> (count xs) 0)
      (list 'if (first xs)
            (if (> (count xs) 1)
              (nth xs 1) (throw \\"odd number of forms to cond\\"))
            (cons 'cond (rest (rest xs)))))))

(def dec (fn (a) (- a 1)))
(def zero? (fn (n) (= 0 n)))
(def identity (fn (x) x))

(defmacro defn
  (fn [name arglist & value]
      `(def ~name (fn ~arglist ~@value))))

(defn next [s]
  (if (= 1 (count s))
    nil
    (rest s)))

(defn reduce [f init xs]
  (if (empty? xs)
    init
    (reduce f (f init (first xs)) (rest xs))))

(defn reverse [coll]
  (reduce conj '() coll))

(defmacro if-not
  (fn [test then else]
    (if else
      `(if (not ~test) ~then ~else)
      `(if-not ~test ~then nil))))

(defn juxt [& f]
  (fn [& a]
    (map #(apply % a) f)))

(defn upper-case [s]
  (. (str "'" s "'" ".toUpperCase")))
