(defn lt [b]
  (let [z (apply max 0
                 (for [b [b (vec (reverse b))]
                       y (range (count b))
                       x (range (inc (/ (Math/log (get b y 0)) (Math/log 2))))
                       [i a m] [[-1 0 0] [-1 0 1] [-1 1 0] [0 1 0] [0 1 1]]
                       :let [s (loop [m m
                                      d [i a]
                                      [l h :as r] [x x]
                                      s 0
                                      [w & e :as b] (drop y b)]
                                 (cond
                                   (and w (>= l 0) (every? #(bit-test w %) (range l (inc h))))
                                   (recur m d (map + d r) (+ s 1 (- h l)) e)
                                   (< h l) s
                                   (= 0 m) s
                                   (= 1 m) (recur 2 (map - d) (map - r d d) s b)))]
                       :when s]
                   s))]
    (when (> z 1) z)))