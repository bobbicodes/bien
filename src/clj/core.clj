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