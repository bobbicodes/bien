(defn key-val [c]
  (loop [[f & r] c, kvm {}]
    (if (nil? f)
      kvm
      (let [[vs l] (split-with (complement keyword?) r)]
        (recur l (assoc kvm f vs))))))