(defn anagram [c]
  (set (for [[_ g] (group-by frequencies c)
             :when (next g)]
         (set g))))