(ns core {:clj-kondo/ignore true})

(defmacro defn [name & fdecl]
  (if (string? (first fdecl))
    `(def ~name (with-meta (fn ~(second fdecl) (do ~@(nnext fdecl))) 
             ~{:doc (first fdecl)}))
    `(def ~name (fn ~(first fdecl) (do ~@(rest fdecl))))))

(defn not [a] (if a false true))

(defn not= [a b] (not (= a b)))

(defmacro cond [& xs]
  (when (> (count xs) 0)
    (list 'if (first xs)
          (if (> (count xs) 1)
            (nth xs 1)
            (throw "odd number of forms to cond"))
          (cons 'cond (rest (rest xs))))))

(defn dec [a] (- a 1))
(defn zero? [n] (= 0 n))
(defn identity [x] x)

(defn next [s]
  (if (or (= 1 (count s)) (= 0 (count s)))
    nil
    (rest s)))

(defn nnext [s]
  (if (or (= 1 (count s)) (= 0 (count s)))
    nil
    (next (next s))))

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

(defmacro if-not [test then else]
  (if else
    `(if (not ~test) ~then ~else)
    `(if-not ~test ~then nil)))

(defn juxt [& f]
  (fn [& a]
    (map #(apply % a) f)))

(defn upper-case [s]
  (. (str "'" s "'" ".toUpperCase")))

(defn _iter-> [acc form]
  (if (list? form)
    `(~(first form) ~acc ~@(rest form))
    (list form acc)))

(defmacro -> [x & xs] (reduce _iter-> x xs))

(defn _iter->> [acc form]
  (if (list? form)
    `(~(first form) ~@(rest form) ~acc) (list form acc)))

(defmacro ->> [x & xs] (reduce _iter->> x xs))

(def gensym-counter
  (atom 0))

(defn gensym [& prefix]
  (symbol (str (if (seq prefix) (first prefix) "G__")
               (swap! gensym-counter inc))))

(defn memoize [f]
  (let [mem (atom {})]
    (fn [& args]
      (let [key (str args)]
        (if (contains? @mem key)
          (get @mem key)
          (let [ret (apply f args)]
            (do (swap! mem assoc key ret)
                ret)))))))

(defn partial [pfn & args]
  (fn [& args-inner]
    (apply pfn (concat args args-inner))))

(defn every? [pred xs]
  (cond (empty? xs)       true
        (pred (first xs)) (every? pred (rest xs))
        true              false))

(defmacro when [x & xs] (list 'if x (cons 'do xs)))

(defmacro if-not [test then else]
  `(if (not ~test) ~then ~else))

(defmacro when-not [test & body]
  (list 'if test nil (cons 'do body)))

(defn fnext [x] (first (next x)))

(defmacro or [& xs]
  (if (empty? xs) nil
      (if (= 1 (count xs))
        (first xs)
        (let [condvar (gensym)]
          `(let [~condvar ~(first xs)]
             (if ~condvar ~condvar (or ~@(rest xs))))))))

(defmacro and [& xs]
  (cond (empty? xs)      true
        (= 1 (count xs)) (first xs)
        true
        (let [condvar (gensym)]
          `(let [~condvar ~(first xs)]
             (if ~condvar (and ~@(rest xs)) ~condvar)))))

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

(defn neg? [n]
  (> 0 n))

(defn even? [n]
  (zero? (mod n 2)))

(defn odd? [n]
  (not (zero? (mod n 2))))

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

(defn mod [num div]
  (let [m (rem num div)]
    (if (or (zero? m) (= (pos? num) (pos? div)))
      m
      (+ m div))))

(defn take-while [pred coll]
  (loop [s (seq coll) res []]
    (if (empty? s) res
        (if (pred (first s))
          (recur (rest s) (conj res (first s)))
          res))))

(defn partition [n step coll]
  (if-not coll
    (partition n n step)
    (loop [s coll p []]
      (if (= 0 (count s))
        (filter #(= n (count %)) p)
        (recur (drop step s)
               (conj p (take n s)))))))

(defn partition-all [n step coll]
  (if-not coll
    (partition-all n n step)
    (loop [s coll p []]
      (if (= 0 (count s)) p
          (recur (drop step s)
                 (conj p (take n s)))))))

(defn partition-by [f coll]
  (loop [s (seq coll) res []]
    (if (= 0 (count s)) res
        (recur (drop (count (take-while (fn [x] (= (f (first s)) (f x))) s)) s)
               (conj res (take-while (fn [x] (= (f (first s)) (f x))) s))))))

(defn coll? [x]
  (or (list? x) (vector? x) (set? x) (map? x)))

(defn group-by [f coll]
  (reduce
   (fn [ret x]
     (let [k (f x)]
       (assoc ret k (conj (get ret k []) x))))
   {} coll))


(defn fromCharCode [int]
  (js-eval (str "String.fromCharCode(" int ")")))

(defn Character/isAlphabetic [int]
  (not= (upper-case (fromCharCode int))
        (lower-case (fromCharCode int))))

(defn Character/isUpperCase [x]
  (if (int? x)
    (and (Character/isLetter (fromCharCode x))
         (= (fromCharCode x)
            (upper-case (fromCharCode x))))
    (and (Character/isLetter x)
         (= x (upper-case x)))))

(defn Character/isLowerCase [x]
  (if (int? x)
    (and (Character/isLetter (fromCharCode x))
         (= (fromCharCode x)
            (lower-case (fromCharCode x))))
    (and (Character/isLetter x)
         (= x (lower-case x)))))

(defn zipmap [keys vals]
  (loop [map {}
         ks (seq keys)
         vs (seq vals)]
    (if-not (and ks vs) map
            (recur (assoc map (first ks) (first vs))
                   (next ks)
                   (next vs)))))

(defn empty [coll]
  (cond
    (list? coll) '()
    (vector? coll) []
    (set? coll) #{}
    (map? coll) {}
    (string? coll) ""))

(defn map1 [f coll]
  (loop [s (seq coll) res []]
    (if (empty? s) res
        (recur (rest s) (conj res (f (first s)))))))

(defn map2 [f c1 c2]
  (loop [s1 (seq c1) s2 (seq c2) res []]
    (if (or (empty? s1) (empty? s2)) res
        (recur (rest s1) (rest s2)
               (conj res (f (first s1) (first s2)))))))

(defn map3 [f c1 c2 c3]
  (loop [s1 (seq c1) s2 (seq c2) s3 (seq c3) res []]
    (if (or (empty? s1) (empty? s2) (empty? s3)) res
        (recur (rest s1) (rest s2) (rest s3)
               (conj res (f (first s1) (first s2) (first s3)))))))

(defn map [f & colls]
  (cond
    (empty? (first colls)) '()
    (= 1 (count colls)) (map1 f (first colls))
    (= 2 (count colls)) (map2 f (first colls) (second colls))
    (= 3 (count colls)) (map3 f (first colls) (second colls) (last colls))
    :else (throw (str "Map not implemented on " (count colls) " colls"))))

(defn drop-last [n coll]
  (if-not coll
    (drop-last 1 n)
    (map (fn [x _] x) coll (drop n coll))))

(defn interleave [c1 c2]
  (loop [s1  (seq c1)
         s2  (seq c2)
         res []]
    (if (or (empty? s1) (empty? s2))
      res
      (recur (rest s1)
             (rest s2)
             (conj res (first s1) (first s2))))))

(defn into [to from]
  (reduce conj to from))

(defmacro if-let [bindings then else & oldform]
  (if-not else
    `(if-let ~bindings ~then nil)
    (let [form (get bindings 0) tst (get bindings 1)
          temp# (gensym)]
      `(let [temp# ~tst]
         (if temp#
           (let [~form temp#]
             ~then)
           ~else)))))

(defn frequencies [coll]
  (reduce (fn [counts x]
            (assoc counts x (inc (get counts x 0))))
          {} coll))

(defn constantly [x] (fn [& args] x))

(defn str/capitalize [s]
  (let [s (str s)]
    (if (< (count s) 2)
      (upper-case s)
      (str (upper-case (subs s 0 1))
           (upper-case (subs s 1))))))

(defn reduce-kv [m f init]
  (reduce (fn [ret kv] (f ret (first kv) (last kv))) init m))

(defmacro when-let [bindings & body]
  (let [form (get bindings 0) tst (get bindings 1)
        temp# (gensym)]
    `(let [temp# ~tst]
       (when temp#
         (let [~form temp#]
           ~@body)))))

(defmacro when-first [bindings & body]
  (let [x   (first bindings)
        xs  (last bindings)
        xs# (gensym)]
    `(when-let [xs# (seq ~xs)]
       (let [~x (first xs#)]
         ~@body))))

(defn Integer/parseInt [s r]
  (if-not r (js-eval (str "parseInt(" s ")"))
          (js-eval (str "parseInt(" s r ")"))))

(defn parseInt [s r]
  (Integer/parseInt s r))

(defmacro for [seq-exprs body-expr]
  (let [body-expr* body-expr
        iter# (gensym)
        to-groups (fn [seq-exprs]
                    (reduce (fn [groups kv]
                              (if (keyword? (first kv))
                                (conj (pop groups) (conj (peek groups) [(first kv) (last kv)]))
                                (conj groups [(first kv) (last kv)])))
                            [] (partition 2 seq-exprs)))
        emit-bind (defn emit-bind [bindings]
                    (let [giter (gensym "iter__")
                          gxs (gensym "s__")
                          iterys# (gensym "iterys__")
                          fs#     (gensym "fs__")
                          do-mod (defn do-mod [mod]
                                   (cond
                                     (= (ffirst mod) :let) `(let ~(second (first mod)) ~(do-mod (next mod)))
                                     (= (ffirst mod) :while) `(when ~(second (first mod)) ~(do-mod (next mod)))
                                     (= (ffirst mod) :when) `(if ~(second (first mod))
                                                    ~(do-mod (next mod))
                                                    (recur (rest ~gxs)))
                                     (keyword?  (ffirst mod)) (throw (str "Invalid 'for' keyword " (ffirst mod)))
                                     (next bindings)
                                     `(let [~iterys# ~(emit-bind (next bindings))
                                            ~fs# (seq (~iterys# ~(second (first (next bindings)))))]
                                        (if ~fs#
                                          (concat ~fs# (~giter (rest ~gxs)))
                                          (recur (rest ~gxs))))
                                     :else `(cons ~body-expr (~giter (rest ~gxs)))))]
                      (if (next bindings)
                        `(defn ~giter [~gxs]
                           (loop [~gxs ~gxs]
                             (when-first [~(ffirst bindings) ~gxs]
                               ~(do-mod (subvec (first bindings) 2)))))
                          `(defn ~giter [~gxs]
                              (loop [~gxs ~gxs]
                                (when-let [~gxs (seq ~gxs)]
                                  (let [~(ffirst bindings) (first ~gxs)]
                                      ~(do-mod (subvec (first bindings) 2)))))))))]
    `(let [~iter# ~(emit-bind (to-groups seq-exprs))]
       (~iter# ~(second seq-exprs)))))

(defn destructure* [bindings]
  (let [bents (partition 2 bindings)
        pb (defn pb [bvec b v]
             (let [pvec
                   (fn [bvec b val]
                     (let [gvec (gensym "vec__")
                           gseq (gensym "seq__")
                           gfirst (gensym "first__")
                           has-rest (some #{'&} b)]
                       (loop [ret (let [ret (conj bvec gvec val)]
                                    (if has-rest
                                      (conj ret gseq (list seq gvec))
                                      ret))
                              n 0
                              bs b
                              seen-rest? false]
                         (if (seq bs)
                           (let [firstb (first bs)]
                             (cond
                               (= firstb '&) (recur (pb ret (second bs) gseq)
                                                    n
                                                    (nnext bs)
                                                    true)
                               (= firstb :as) (pb ret (second bs) gvec)
                               :else (if seen-rest?
                                       (throw "Unsupported binding form, only :as can follow & parameter")
                                       (recur (pb (if has-rest
                                                    (conj ret
                                                          gfirst `(~first ~gseq)
                                                          gseq `(~next ~gseq))
                                                    ret)
                                                  firstb
                                                  (if has-rest
                                                    gfirst
                                                    (list nth gvec n nil)))
                                              (inc n)
                                              (next bs)
                                              seen-rest?))))
                           ret))))
                   pmap
                   (fn [bvec b v]
                     (let [gmap (gensym "map__")
                           defaults (:or b)]
                       (loop [ret (-> bvec (conj gmap) (conj v)
                                      (conj gmap) (conj (list 'if (list seq? gmap)
                                                              `(seq-to-map-for-destructuring ~gmap)
                                                              gmap))
                                      ((fn [ret]
                                         (if (:as b)
                                           (conj ret (:as b) gmap)
                                           ret))))
                              bes (let [transforms
                                        (reduce
                                         (fn [transforms mk]
                                           (if (keyword? mk)
                                             (let [mkns (namespace mk)
                                                   mkn (name mk)]
                                               (cond (= mkn "keys") (assoc transforms mk #(keyword (or mkns (namespace %)) (name %)))
                                                     (= mkn "syms") (assoc transforms mk #(list `quote (symbol (or mkns (namespace %)) (name %))))
                                                     (= mkn "strs") (assoc transforms mk str)
                                                     :else transforms))
                                             transforms))
                                         {}
                                         (keys b))]
                                    (reduce
                                     (fn [bes entry]
                                       (reduce #(assoc %1 %2 ((val entry) %2))
                                               (dissoc bes (key entry))
                                               ((key entry) bes)))
                                     (dissoc b :as :or)
                                     transforms))]
                         (if (seq bes)
                           (let [bb (key (first bes))
                                 bk (val (first bes))
                                 local (if #?(:clj  (instance? clojure.lang.Named bb)
                                              :cljs (implements? INamed bb))
                                         (with-meta (symbol nil (name bb)) (meta bb))
                                         bb)
                                 bv (if (contains? defaults local)
                                      (list `get gmap bk (defaults local))
                                      (list `get gmap bk))]
                             (recur
                              (if (or (keyword? bb) (symbol? bb))
                                (-> ret (conj local bv))
                                (pb ret bb bv))
                              (next bes)))
                           ret))))]
               (cond
                 (symbol? b) (-> bvec (conj (if (namespace b)
                                              (symbol (name b)) b)) (conj v))
                 (keyword? b) (-> bvec (conj (symbol (name b))) (conj v))
                 (vector? b) (pvec bvec b v)
                 (map? b) (pmap bvec b v)
                 :else (throw (str "Unsupported binding form: " b)))))
        process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
    (println "(pb bvec (first b) (second b)):" (pb bvec (first b) (second b)))
    (if (every? symbol? (map first bents))
      bindings
      (if-let [kwbs (seq (filter #(keyword? (first %)) bents))]
        (throw (str "Unsupported binding key: " (ffirst kwbs)))
        (reduce process-entry [] bents)))))


#_(destructure* '[[a b] ["a" "b"]])

