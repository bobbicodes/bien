- Ah, here we go. I need to understand what `seq-to-map-for-destructuring` does.
- I'm looking at a simple associative binding: `[{:keys [:w :b]} coords]`
- ```clojure
  [map__1189 coords
   map__1189 (if (seq? map__1189)
                 (seq-to-map-for-destructuring map__1189)
                 map__1189)
   w (get map__1189 :w)
   b (get map__1189 :b)]
  ```
- Let's back up and look at the problem, which is `queen attack`. It's a hard problem, and it's chess, but it doesn't matter. This is the first part of the solution:
- ```clojure
  (defn cell-string [{:keys [:w :b]} coords]
    (condp = coords
      w "W"
      b "B"
      "_"))
  
  (defn- row-string [queens row]
    (->> (range 8)
         (map #(cell-string queens [row, %]))
         (clojure.string/join " ")))
  
  (defn board-string [queens]
    (->> (range 8)
         (map #(row-string queens %))
         (map #(str % "\n"))
         str/join))
  ```
- This builds the board with 2 queens at the given coords:
- ```clojure
  (board-string {:w [2 4] :b [6 6]})
  
  (str "_ _ _ _ _ _ _ _\n"
       "_ _ _ _ _ _ _ _\n"
       "_ _ _ _ W _ _ _\n"
       "_ _ _ _ _ _ _ _\n"
       "_ _ _ _ _ _ _ _\n"
       "_ _ _ _ _ _ _ _\n"
       "_ _ _ _ _ _ B _\n"
       "_ _ _ _ _ _ _ _\n")
  ```
- And finally, the `can-attack?` fn:
- ```clojure
  (defn can-attack [{:keys [:w :b]}]
    (let [diffs (map #(Math/abs (- %1 %2)) w b)]
      (or (some zero? diffs)
          (apply = diffs))))
  ```
- Now I'm confused, because that destructuring form doesn't seem to expand to anything:
- ```clojure
  (destructure* '[{:keys [:w :b]}])
  
  [{:keys [:w :b]}]
  ```
- So... how does it work? I don't get it, it seems like I'm missing something.
- If we back up, and look at a "normal" map binding from the docs:
- ```clojure
  (destructure* '[{name :name
                  location :location
                  description :description} client])
  
  [map__1741 client
   map__1741 (if (seq? map__1741)
                 (seq-to-map-for-destructuring map__1741)
                  map__1741)
   name (get map__1741 :name)
   location (get map__1741 :location)
   description (get map__1741 :description)]
  ```
- Back up again... what is `client`? Well, kinda the whole point is we don't know. But it's this:
- ```clojure
  (def client {:name "Super Co."
               :location "Philadelphia"
               :description "The worldwide leader in plastic tableware."})
  ```
- `(seq? client)` evaluates to false. It's already a map so it doesn't need to be made into one. The seq-to-map business must be for mixing sequential/associative destructuring.
- Ah. I remember now, there was a comment in the code for seq-to-map... that referenced this: https://clojure.org/guides/destructuring#_keyword_arguments
- It's only needed to allow stuff like `(configure 12 :debug true)`. It's a sequence but it's destructured like a map. So that means we can skip it for now! It's a special case. Glad I figured that out. This session was worth it just for that.
- The kicker is, I think, that our verbose subject is basically just assoc. But it's buried in the java hashmap data structure or something.
- So the first loop variable, `ret`, would go from this:
- ```clojure
  (-> bvec (conj gmap) (conj v)
                         (conj gmap) (conj (list 'if (list sequential? gmap)
                                                 `(seq-to-map-for-destructuring ~gmap)
                                                 gmap))
                         ((fn [ret]
                            (if (:as b)
                              (conj ret (:as b) gmap)
                              ret))))
  ```
- To this:
- ```clojure
  (-> bvec (conj gmap) (conj v)
                         (conj gmap) (conj gmap)
                         ((fn [ret]
                            (if (:as b)
                              (conj ret (:as b) gmap)
                              ret))))
  ```
- `bvec` starts out as an empty vector. That's what populates it.
- You get the sense that this likely started out much simpler, and this was added or something.
- The very last section of the page is super insightful: https://clojure.org/guides/destructuring#_macros
- It mentions the undocumented destructure function and shows an example:
- ```clojure
  (destructure '[[x & remaining :as all] numbers])
  ;= [vec__1 numbers
  ;=  x (clojure.core/nth vec__1 0 nil)
  ;=  remaining (clojure.core/nthnext vec__1 1)
  ;=  all vec__1]
  ```
- I wish I had noticed that earlier. It's very obvious now because I spent weeks figuring it out myself.
- improving the `for` macro by breaking out helpers
- Was only partly successful since `do-mod` needs like everything. But it felt wonderful to get the big `emit` function out of there. The real solution is named lambdas, but I don't see it causing any major issues in the meantime. Unless someone wants to name a fn `do-mod`
- Somewhere between 138-143 tests passing. Some seem to fail depending on the order or something, because my attempt at having each test initialize a fresh env didn't work.