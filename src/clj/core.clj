(ns core {:clj-kondo/ignore true})

(defmacro defn
  (fn [name arglist & value]
    `(def ~name (fn ~arglist ~@value))))

(defn not [a] (if a false true))

(defn not= [a b] (not (= a b)))

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

(defn next [s]
  (if (= 1 (count s))
    nil
    (rest s)))

(defn reduce [f init xs]
  (if (empty? xs)
    init
    (reduce f (f init (first xs)) (rest xs))))

(defn reductions [f init xs]
  (loop [s xs acc init res [init]]
    (if (empty? s)
      res
      (recur (rest s) 
             (f acc (first s))
             (conj res (f acc (first s)))))))

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

(defn _iter-> [acc form]
  (if (list? form)
    `(~(first form) ~acc ~@(rest form))
    (list form acc)))

(defmacro -> (fn [x & xs] (reduce _iter-> x xs)))

(defn _iter->> [acc form]
  (if (list? form)
    `(~(first form) ~@(rest form) ~acc) (list form acc)))

(defmacro ->> (fn (x & xs) (reduce _iter->> x xs)))

(def gensym
  (let [counter (atom 0)]
    (fn []
      (symbol (str "G__" (swap! counter inc))))))

(defn memoize [f]
  (let [mem (atom {})]
    (fn [& args]
      (let [key (str args)]
        (if (contains? @mem key)
          (get @mem key)
          (let [ret (apply f args)]
            (do
              (swap! mem assoc key ret)
              ret)))))))

(defn partial [pfn & args]
  (fn [& args-inner]
    (apply pfn (concat args args-inner))))

(defn every? [pred xs]
  (cond (empty? xs)       true
        (pred (first xs)) (every? pred (rest xs))
        true              false))

(defmacro when (fn [x & xs] (list 'if x (cons 'do xs))))

(defmacro if-not
  (fn [test then else]
    `(if (not ~test) ~then ~else)))

(defmacro when-not
  (fn [test & body]
    (list 'if test nil (cons 'do body))))

(defn fnext [x] (first (next x)))

(defmacro or
  (fn [& xs]
    (if (empty? xs) nil
        (if (= 1 (count xs))
          (first xs)
          (let [condvar (gensym)]
            `(let [~condvar ~(first xs)]
               (if ~condvar ~condvar (or ~@(rest xs)))))))))

(defmacro and
  (fn [& xs]
    (cond (empty? xs)      true
          (= 1 (count xs)) (first xs)
          true
          (let [condvar (gensym)]
            `(let [~condvar ~(first xs)]
               (if ~condvar (and ~@(rest xs)) ~condvar))))))

(defn ffirst [x] (first (first x)))

(defn second [l] (nth l 1))

(defn some [pred xs]
  (if (empty? xs)
    nil
    (or (pred (first xs))
        (some pred (rest xs)))))

(defn not-any? [pred coll]
  (not (some pred coll)))

(defn quot [n d]
  (int (/ n d)))

(defn pos? [n]
  (> n 0))

(defn complement [f]
  (fn [x y & zs]
    (if zs
      (not (apply f x y zs))
      (if y
        (not (f x y))
        (if x
          (not (f x))
          (not (f)))))))

(defn mapcat [f & colls]
   (apply concat (apply map f colls)))

(defn remove [pred coll]
  (filter (complement pred) coll))

(defn tree-seq [branch? children node]
  (remove nil?
          (cons node
                (when (branch? node)
                  (mapcat (fn [x] (tree-seq branch? children x)) 
                          (children node))))))
