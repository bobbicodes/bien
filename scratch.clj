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

(def day-structure
  {1 :sunday 2 :monday 3 :tuesday 4 :wednesday
   5 :thursday 6 :friday 7 :saturday})

(defn leap-year? [year]
  (cond (zero? (mod year 400)) true
        (zero? (mod year 100)) false
        :else  (zero? (mod year 4))))

(defn zellers-congruence [input_year input_month input_day]
  (let [month (+ (mod (+ input_month 9) 12) 3)
        year (- input_year (quot (- month input_month) 12))
        century (quot year 100)
        century-year (mod year 100)]
    (mod (+ input_day
            (quot (* 26 (inc month)) 10)
            century-year
            (quot century-year 4)
            (quot century 4)
            (* 5 century)) 7)))

(defn get-day-counts [year]
  {1 31, 2 (if (leap-year? year) 29 28), 3 31, 4 30
   5 31, 6 30, 7 31, 8 31, 9 30, 10 31, 11 30, 12 31})

(defn get-days
  ([year month]
   (get-days year month
             (zellers-congruence year month 1)
             (get-in (get-day-counts year) [month])))
  ([year month start-day limit]
   (loop [count 2
          day (inc start-day)
          day-arrangement {1 (get-in day-structure [start-day])}]
     (if (not= count (inc limit))
       (recur  (inc count)
               (if (= (inc day) 8) 1 (inc day))
               (assoc day-arrangement count
                      (get-in day-structure [day])))
       day-arrangement))))

(def days (get-days year month))

(apply hash-map (flatten (filter #(-> % val (= day)) days)))



(sort-by first
         (get-days year month))

(defn filter-by-day [year month day]
  (let [days (get-days year month)]
    (apply hash-map (flatten (filter #(-> % val (= day)) days)))))

(defn filter-keys [year month day style]
  (let [days (filter-by-day year month day)
        dates (sort (keys days))]
    (cond
      (= style :first)
      (nth dates 0)
      (= style :second)
      (nth dates 1)
      (= style :third)
      (nth dates 2)
      (= style :fourth)
      (nth dates 3)
      (= style :last)
      (nth dates (dec (count dates)))
      (= style :teenth)
      (first (filter #(and (> % 12) (< % 20)) (vec dates))))))

(def month 3)
(def year 2013)
(def day :monday)
(def style :first)

(filter-by-day year month day)

(filter-keys year month day style)

(defn meetup [month year day style]
  [year month (filter-keys year month day style)])

(= [2013 3 4] (meetup 3 2013 :monday :first))

(require '[clojure.set :as set])

(def grid
  ["  B  "
   " B B "
   "B W B"
   " W W "
   "  W  "])

(defn grid->board [grid]
  (->> (apply mapv vector grid)
       (mapv #(mapv {" " nil "B" :black "W" :white} %))))

(def board (grid->board grid))
(def point [0 1])
(def points #{point})

(loop [points #{point}]
  (let [new-points (->> (mapcat (partial neighbors board) points)
                        (filter #(nil? (get-in board %)))
                        set
                        (set/union points))]
    (cond
      (get-in board point)  #{}
      (= points new-points) points
      :else                 (recur new-points))))

(defn invalid? [board [x y]]
  (or (neg? x)
      (neg? y)
      (>= x (count board))
      (>= y (count (first board)))))

(defn neighbors [board [x y]]
  (->> [[x (dec y)] [x (inc y)] [(inc x) y] [(dec x) y]]
       (remove (partial invalid? board))))



(def board (grid->board grid))
(def point [0 1])
(def points #{point})

(def new-points
  (->> (mapcat (partial neighbors board) points)
       (filter #(nil? (get-in board %)))
       set
       (set/union points)))


(set/union
 (set
  (filter #(nil? (get-in board %))
          (mapcat (partial neighbors board) points)))
 points)

(loop [points #{point}]
  (let [new-points (->> (mapcat (partial neighbors board) points)
                        (filter #(nil? (get-in board %)))
                        set
                        (set/union points))]
    (cond
      (get-in board point)  #{}
      (= points new-points) points
      :else                 (recur new-points))))


(def new-points
  (->> (mapcat (partial neighbors board) points)
       (filter #(nil? (get-in board %)))
       set
       (set/union points)))

(defn point->territory [board point]
  (loop [points #{point}]
    (let [new-points (->> (mapcat (partial neighbors board) points)
                          (filter #(nil? (get-in board %)))
                          set
                          (set/union points))]
      (cond
        (get-in board point)  #{}
        (= points new-points) points
        :else                 (recur new-points)))))

(point->territory (grid->board grid) [0 1])

(defn territory [grid [x y]]
  (let [board (grid->board grid)
        t     (point->territory board [x y])]
    {:stones t
     :owner  (->> t
                  (mapcat (partial neighbors board))
                  (map (partial get-in board))
                  (remove nil?)
                  set
                  (get {#{:black} :black #{:white} :white}))}))

(territory example [0 1])

(defn territories [grid]
  (let [ts (->> (for [y (range (count grid))
                      x (range (count (first grid)))]
                  [x y])
                (mapv (partial territory grid))
                (map (fn [m] {(:owner m) (:stones m)}))
                (apply (partial merge-with set/union)))]
    {:black-territory (get ts :black #{})
     :white-territory (get ts :white #{})
     :null-territory  (get ts nil)}))

(def mycolls
  [[1 2] [3 4] [5 6] [7 8] [9 0]])

(def f str)
(def c1 [1 2])
(def c2 [3 4])
(def c3 [5 6])
(def colls [[7 8] [9 0]])
(def cs (conj colls c3 c2 c1))

(def c ["meat" "mat" "team" "mate" "eat"])

(group-by frequencies c)

{{\m 1, \e 1, \a 1, \t 1} 
 ["meat" "team" "mate"], 
 {\m 1, \a 1, \t 1} ["mat"], 
 {\e 1, \a 1, \t 1} ["eat"]}

(macroexpand
 '(for [x (range 3) y (range 3) :while (not= x y)] [x y]))

;; expansions of call to `for` that fails, followed by one that works:

(let* [G__2561 (defn iter__2562 [s__2563]
                 (loop [s__2563 s__2563]
                   (when-let [s__2563 (seq s__2563)]
                     (let [[x y] (first s__2563)]
                       (if (= y 0)
                         (cons x (iter__2562 (rest s__2563)))
                         (#function[do-mod] (rest s__2563)))))))]
      (remove nil?
              (G__2561 (quote ([:a 1] [:b 2] [:c 0])))))
 
(let* [G__2849 (defn iter__2850 [s__2851]
                  (loop [s__2851 s__2851]
                    (when-first [x s__2851]
                      (when (not= x 1)
                        (let [iterys__2852 (defn iter__2868 [s__2869]
                                             (loop [s__2869 s__2869]
                                               (when-let [s__2869 (seq s__2869)]
                                                 (let [y (first s__2869)]
                                                   (cons [x y] (iter__2868 (rest s__2869)))))))
                              fs__2853     (seq (iterys__2852 (range 3)))]
                          (if fs__2853
                            (concat fs__2853 (iter__2850 (rest s__2851)))
                            (#function[do-mod] (rest s__2851))))))))]
  (remove nil? (G__2849 (range 3))))

(def expr "What is 1 plus 1?")

(re-matches #"What is (.+)\?" expr)

(partition-all 2 ["plus" "1"])