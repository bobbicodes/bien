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

(destructure* '[{:keys [:w :b]} coords])
(destructure* '[{:keys [:w :b]}])

(def client {:name "Super Co."
             :location "Philadelphia"
             :description "The worldwide leader in plastic tableware."})

(seq? client)

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

(defn abs [n]
  (if (neg? n) (- n) n))

(def empty-board
  (->> ["_" "_" "_" "_" "_" "_" "_" "_"]
       (repeat 8)
       vec))

(defn board->str [board]
  (->> board
       (map #(str/join " " %))
       (map #(str % "\n"))
       (apply str)))

(defn board-string [{:keys [w b]}]
  (-> empty-board
      (cond-> w (assoc-in w \W)
              b (assoc-in b \B))
      board->str))

(defn can-attack [{[wx wy] :w [bx by] :b :as state}]
  (or (= wx bx)
      (= wy by)
      (= (abs (- wx bx))
         (abs (- wy by)))))

(let [{:keys [w b]} {:w [2 4] :b [6 6]}]
  [w b])

(defn powerset [s]
  (reduce 
   (fn [x y] 
     (into x 
           (for [subset x] 
             (conj subset y))))
   #{#{}} 
   s))

(count (powerset (into #{} (range 10))))

(def n 2)
(def step 2)
(def pad [0])
(def coll '(9 5))
(def s coll)
(def p [])

(loop [s coll p []]
  (if (= n (count (take n s)))
    (recur (drop step s) (conj p (take n s)))
    (conj p (concat (take n s) pad))))

(take 2 '())