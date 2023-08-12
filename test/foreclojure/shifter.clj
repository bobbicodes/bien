(defn shifter [n s]
  (take (count s) (drop (mod n (count s)) (cycle s))))