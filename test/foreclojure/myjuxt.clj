(defn myjuxt [& f]
  (fn [& a]
    (map #(apply % a) f)))