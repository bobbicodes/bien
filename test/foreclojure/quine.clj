(defn quine []
  (fn []
    (let [x '(list 'fn []
                   (list 'let ['x (list 'quote x)]
                         (list 'str x)))]
      (str (list 'fn []
                 (list 'let ['x (list 'quote x)]
                       (list 'str x)))))))