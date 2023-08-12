(defn conway [board]
  (let [r (range (count board))
        v [-1 0 1]
        a \#]
    (for [y r]
      (apply str (for [x r c [(count
                               (for [j v
                                     k v
                                     :when (= a (get-in board [(+ y j) (+ x k)]))]
                                 1))]]
                   (if (or (= c 3) (and (= c 4) (= a (get-in board [y x]))))
                     a
                     \ ))))))