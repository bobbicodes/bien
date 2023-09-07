(ns pprint {:clj-kondo/ignore true})

(def *print-pretty* true)
(def *print-pprint-dispatch* nil)
(def *print-right-margin* 72)
(def *print-miser-width* 40)
(def *print-lines* nil)
(def *print-circle* nil)
(def *print-shared* nil)
(def *print-suppress-namespaces* nil)
(def *print-radix* nil)
(def *print-base* 10)
(def *current-level* 0)
(def *current-length* nil)
(def format-simple-number)

(defn spaces- [indent]
     (if (> indent 0)
       (str " " (spaces- (- indent 1)))
       ""))

(defn pp-seq- [obj indent]
     (let* [xindent (+ 1 indent)]
           (apply str (pp- (first obj) 0)
                  (map (fn [x] (str "\n" (spaces- xindent)
                                     (pp- x xindent)))
                       (rest obj)))))

(defn pp-map- [obj indent]
     (let* [ks (keys obj)
            kindent (+ 1 indent)
            kwidth (count (seq (str (first ks))))
            vindent (+ 1 (+ kwidth kindent))]
           (apply str (pp- (first ks) 0)
                  " "
                  (pp- (get obj (first ks)) 0)
                  (map (fn [k] (str "\n" (spaces- kindent)
                                     (pp- k kindent)
                                     " "
                                     (pp- (get obj k) vindent)))
                       (rest (keys obj))))))

(defn pp- [obj indent]
     (cond
       (list? obj)   (str "(" (pp-seq- obj indent) ")")
       (vector? obj) (str "[" (pp-seq- obj indent) "]")
       (set? obj) (str "#{" (pp-seq- obj indent) "}")
       (map? obj)    (str "{" (pp-map- obj indent) "}")
       :else         (pr-str obj)))

(defn pprint [obj]
  (pp- obj 0))