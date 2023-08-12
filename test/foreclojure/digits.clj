(defn digits [x y]
  (map #(- (int %) (int \0)) (str (* x y))))