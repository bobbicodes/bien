(require '[babashka.fs :as fs]
         '[clojure.string :as str]
         '[cheshire.core :as json]
         '[clojure.test :refer [is]])

(def foreclojure-exercises
  (map #(subs (str/replace % ".clj" "") 17)
       (fs/list-dir "test\\foreclojure")))

(def ex-test
  (group-by #(str/ends-with? % "_test") 
            foreclojure-exercises))

(def exercises (flatten (nfirst ex-test)))
(def tests (flatten (rest (last ex-test))))

(def src-all
  (for [slug exercises]
    (let [filename (str/replace slug "-" "_")
          f (fs/file "test" "foreclojure" (str filename ".clj"))]
      (slurp f))))

(first exercises)

(let [slug "anagram"
      filename (str/replace slug "-" "_")
      f (fs/file "test" (str filename ".clj"))]
  f
  #_(slurp f))

(def test-all
  (for [slug tests]
    (let [filename (str/replace slug "-" "_")
          f (fs/file "test" "foreclojure" (str filename ".clj"))]
      (slurp f))))

(def foreclojure
  (map #(slurp (fs/file "test\\foreclojure\\" (str % ".clj")))
       foreclojure-exercises))

(first foreclojure-exercises)

(map str (fs/list-dir "test\\foreclojure"))

(def exercises
  (map #(subs (str/replace % ".clj" "") 15)
       (fs/list-dir "exercise_tests")))



(comment
  (spit "foreclojure-exercises.json"
        (json/generate-string
         (zipmap exercises src-all)
         {:pretty true}))

 (spit "foreclojure-tests.json"
      (json/generate-string
       (zipmap tests test-all)
       {:pretty true}))
  (spit "tests.json"
        (json/generate-string
         (zipmap exercises test-all)
         {:pretty true}))
  (spit "instructions.json"
        (json/generate-string
         (zipmap (map #(str/replace % "-" "_") practice-exercises)
                 instructions-all)
         {:pretty true}))
  (spit "solutions.json"
        (json/generate-string
         (zipmap (map #(str/replace % "-" "_") practice-exercises)
                 solutions-all)
         {:pretty true}))
  (spit "foreclojure-solutions.json"
        (json/generate-string
         (zipmap foreclojure-exercises
                 foreclojure)
         {:pretty true}))
  (spit "foreclojure-tests.json"
        (json/generate-string
         (zipmap foreclojure-problems
                 foreclojure-tests)
         {:pretty true})))

(map not= [1 2 3] ["a" "b" "c"])

(defn destructure* [bindings]
  (let [bents (partition 2 bindings)
        pb (fn pb [bvec b v]
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
                                       (throw #?(:clj (new Exception "Unsupported binding form, only :as can follow & parameter")
                                                 :cljs (new js/Error "Unsupported binding form, only :as can follow & parameter")))
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
                                                              `(clojure.core/seq-to-map-for-destructuring ~gmap)
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
                              (if (or (keyword? bb) (symbol? bb)) ;(ident? bb)
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
                 :else (throw
                        #?(:clj (new Exception (str "Unsupported binding form: " b))
                           :cljs (new js/Error (str "Unsupported binding form: " b)))))))
        process-entry (fn [bvec b] (pb bvec (first b) (second b)))]
    (if (every? symbol? (map first bents))
      bindings
      (if-let [kwbs (seq (filter #(keyword? (first %)) bents))]
        (throw
         #?(:clj (new Exception (str "Unsupported binding key: " (ffirst kwbs)))
            :cljs (new js/Error (str "Unsupported binding key: " (ffirst kwbs)))))
        (reduce process-entry [] bents)))))

(destructure* '[[[bind expr & mod-pairs] & next-groups]])

(def client {:name "Super Co."
             :location "Philadelphia"
             :description "The worldwide leader in plastic tableware."})

(destructure* '[{name :name
                location :location
                description :description} client])

(defn update-ranges
  "Applies `f` to each range in `state` (see `changeByRange`)"
  ([state f]
   (update-ranges state nil f))
  ([^js state tr-specs f]
   (->> (fn [range]
          (or (when-some [result (f range)]
                (map-cursor range state result))
              #js{:range range}))
        (.changeByRange state)
        (#(j/extend! % tr-specs))
        (.update state))))

(defn slurp [direction]
  (fn [^js state]
    (update-ranges
     state
     (j/fn [^:js {:as   range
                  :keys [from to empty]}]
       (when empty
         (when-let [parent
                    (n/closest (n/tree state from)
                               (every-pred n/coll?
                                           #(not
                                             (some-> % n/with-prefix n/right n/end-edge?))))]
           (when-let [target (first (remove n/line-comment? (n/rights (n/with-prefix parent))))]
             {:cursor/mapped from
              :changes       (let [edge (n/down-last parent)]
                               [{:from   (-> target n/end)
                                 :insert (n/name edge)}
                                (-> edge
                                    n/from-to
                                    (j/assoc! :insert " "))])})))))))

(defmacro for [seq-exprs body-expr]
  (let [to-groups (fn [seq-exprs]
                    (reduce (fn [groups [k v]]
                               (if (keyword? k)
                                 (conj (pop groups) (conj (peek groups) [k v]))
                                 (conj groups [k v])))
                             [] (partition 2 seq-exprs)))
        err (fn [& msg] (throw (IllegalArgumentException. ^String (apply str msg))))
        emit-bind (fn emit-bind [[[bind expr & mod-pairs]
                                  & [[_ next-expr] :as next-groups]]]
                    (let [giter (gensym "iter__")
                          gxs (gensym "s__")
                          do-mod (fn do-mod [[[k v :as pair] & etc]]
                                   (cond
                                     (= k :let) `(let ~v ~(do-mod etc))
                                     (= k :while) `(when ~v ~(do-mod etc))
                                     (= k :when) `(if ~v
                                                    ~(do-mod etc)
                                                    (recur (rest ~gxs)))
                                     (keyword? k) (err "Invalid 'for' keyword " k)
                                     next-groups
                                     `(let [iterys# ~(emit-bind next-groups)
                                            fs# (seq (iterys# ~next-expr))]
                                        (if fs#
                                          (concat fs# (~giter (rest ~gxs)))
                                          (recur (rest ~gxs))))
                                     :else `(cons ~body-expr
                                                  (~giter (rest ~gxs)))))]
                      (if next-groups
                        #_"not the inner-most loop"
                        `(fn ~giter [~gxs]
                           (lazy-seq
                            (loop [~gxs ~gxs]
                              (when-first [~bind ~gxs]
                                ~(do-mod mod-pairs)))))
                        #_"inner-most loop"
                        (let [gi (gensym "i__")
                              gb (gensym "b__")
                              do-cmod (fn do-cmod [[[k v :as pair] & etc]]
                                        (cond
                                          (= k :let) `(let ~v ~(do-cmod etc))
                                          (= k :while) `(when ~v ~(do-cmod etc))
                                          (= k :when) `(if ~v
                                                         ~(do-cmod etc)
                                                         (recur
                                                          (unchecked-inc ~gi)))
                                          (keyword? k)
                                          (err "Invalid 'for' keyword " k)
                                          :else
                                          `(do (chunk-append ~gb ~body-expr)
                                               (recur (unchecked-inc ~gi)))))]
                          `(fn ~giter [~gxs]
                             (lazy-seq
                              (loop [~gxs ~gxs]
                                (when-let [~gxs (seq ~gxs)]
                                  (if (chunked-seq? ~gxs)
                                    (let [c# (chunk-first ~gxs)
                                          size# (int (count c#))
                                          ~gb (chunk-buffer size#)]
                                      (if (loop [~gi (int 0)]
                                            (if (< ~gi size#)
                                              (let [~bind (.nth c# ~gi)]
                                                ~(do-cmod mod-pairs))
                                              true))
                                        (chunk-cons
                                         (chunk ~gb)
                                         (~giter (chunk-rest ~gxs)))
                                        (chunk-cons (chunk ~gb) nil)))
                                    (let [~bind (first ~gxs)]
                                      ~(do-mod mod-pairs)))))))))))]
    `(let [iter# ~(emit-bind (to-groups seq-exprs))]
       (iter# ~(second seq-exprs)))))

(declare do-mod)
(declare do-mod*)

(defn emit-bind [bindings body-expr]
  (let [giter (gensym)
        gxs (gensym)]
    (println "bindings:" bindings)
    (if (next bindings)
      `(defn ~giter [~gxs]
         (loop [~gxs ~gxs]
           (when-first [~(ffirst bindings) ~gxs]
             ~(do-mod (subvec (first bindings) 2) body-expr bindings giter gxs))))
      `(defn ~giter [~gxs]
         (loop [~gxs ~gxs]
           (when-let [~gxs (seq ~gxs)]
             (let [~(ffirst bindings) (first ~gxs)]
               ~(do-mod (subvec (first bindings) 2) body-expr bindings giter gxs))))))))

(defn do-mod [domod body-expr bindings giter gxs]
  (cond
    (= (ffirst domod) :let) `(let ~(second (first domod)) ~(do-mod (next domod) body-expr bindings giter gxs))
    (= (ffirst domod) :while) `(when ~(second (first domod)) ~(do-mod (next domod) body-expr bindings giter gxs))
    (= (ffirst domod) :when) `(if ~(second (first domod))
                                ~(do-mod (next domod) body-expr bindings giter gxs)
                                (recur (rest ~gxs)))
    (keyword? (ffirst domod)) (str "Invalid 'for' keyword " (ffirst domod))
    (next bindings)
    `(let [iterys# ~(emit-bind (next bindings) body-expr)
           fs# (seq (iterys# ~(second (first (next bindings)))))]
       (if fs#
         (concat fs# (~giter (rest ~gxs)))
         (recur (rest ~gxs))))
    :else `(cons ~body-expr
                 (~giter (rest ~gxs)))))

(defn do-mod* [domod body-expr bindings giter gxs]
  (if (next bindings)
    `(let [iterys# ~(emit-bind (next bindings) body-expr)
           fs#     (seq (iterys# ~(second (first (next bindings)))))]
       (if fs#
         (concat fs# (~giter (rest ~gxs)))
         (recur (rest ~gxs))))
    `(cons ~body-expr
           (~giter (rest ~gxs)))))

(defmacro for* [seq-exprs body-expr]
  (let [to-groups (fn [seq-exprs]
                    (reduce (fn [groups kv]
                              (if (keyword? (first kv))
                                (conj (pop groups) (conj (peek groups) [(first kv) (last kv)]))
                                (conj groups [(first kv) (last kv)])))
                            [] (partition 2 seq-exprs)))]
   `(let [iter# ~(emit-bind (to-groups seq-exprs) body-expr)]
     (iter# ~(second seq-exprs)))))
