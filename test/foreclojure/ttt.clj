(defn ttt [board]
  (some {[:x :x :x] :x [:o :o :o] :o}
        (concat board (apply map list board)
                (for [d [[[0 0] [1 1] [2 2]] [[2 0] [1 1] [0 2]]]]
                  (for [[x y] d] ((board x) y))))))