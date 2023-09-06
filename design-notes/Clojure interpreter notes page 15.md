- We need this to work:
- ```clojure
  (some #{'&} '[a & b])
  => nil 
  ```
- That doesn't work because this doesn't work:
- ```clojure
  (contains? #{'&} '&)
  => false 
  ```
- Hmm this is weird. My implementation of `contains?` is now correct, but it only works *without* the quote:
- ```clojure
  (contains? #{&} '&)
  => true 
  ```
- This is because inside the set, `'&` becomes `(quote &)`. This might be something I want to get to the bottom of.
- Ah, I think I just need to have sets evaluate their elements. Yes, similar to what I did for maps.
- Cool, so that's done. So now I can proceed to track down where this fails:
- ```clojure
  (def client {:name "Super Co."
               :location "Philadelphia"
               :description "The worldwide leader in plastic tableware."})
    
  (let [{:keys [name location description]} client]
    [name location description])
  => 
  Error: 'location' not found
  ```
- Ok let's try to zero in on the problem. We want to convert this
- ```clojure
  {:keys [name location description]}
  ```
- To this
- ```clojure
  {name :name, location :location, description :description}
  ```
- That's what this transform function is supposed to do:
- ```clojure
  (fn [k] (keyword (or mkns (namespace k)) (name k)))
  ```
- Cool. I minimized the failing case to:
- ```clojure
  (def transforms
    (reduce 
     (fn [transforms mk] (assoc transforms mk (fn [k] (keyword k))))
     {}
     '(:keys)))
  
  (reduce
   (fn [bes entry]
     (reduce
      (fn [a b]
        (assoc a b ((val entry) b)))
      (dissoc bes (key entry))
      ((key entry) bes)))
   {:keys '[name location description]}
   transforms)
  => 
  Error: 'location' not found 
  ```
- In Clojure, it correctly outputs `{name :name, location :location, description :description}`. Now I feel like I'm getting somewhere.
- The weird thing is, this works:
- ```clojure
  ((val (first transforms))
   (first '[location description]))
  => :location 
  ```
- Aha. It's simply because `((key entry) bes)` doesn't work. It does if I do `(:keys bes)`
- How can I fix that?
- It's because `entry` is a map entry.
- So it works if I do this:
- ```clojure
  (reduce
   (fn [bes entry]
     (reduce
      (fn [a b]
        (assoc a b ((val entry) b)))
      (dissoc bes (key entry))
      (get bes (key entry))))
   {:keys '[name location description]}
   transforms)
  => {name :name location :location description :description}
  ```
- So I could change it and call it done, and the destructuring syntax would work.
- But it would be better to get to the bottom of why we can't do
- ```clojure
  ((key (first {:keys '[yo whaat sup]})) bes)
  => 
  Error: 'location' not found 
  ```
- But we can do
- ```clojure
  (get bes (key (first {:keys '[yo whaat sup]})))
  => [location description] 
  ```
- Anyway... this works now
- ```clojure
  (def client {:name "Super Co."
               :location "Philadelphia"
               :description "The worldwide leader in plastic tableware."})
    
  (let [{:keys [name location description]} client]
    [name location description])
  => ["Super Co." "Philadelphia" "The worldwide leader in plastic tableware."]
  ```
- So destructuring is pretty much done! Totally confused about this map key thing though.
- Here it is put more generically so I can make an issue, and close the destructuring one.
- ```clojure
  ((first [:keys])
   {:keys '[name location description]})
  ```
- It looks like `:or` isn't working:
- ```clojure
  (def client {:name "Super Co."
               :location "Philadelphia"
               :description "The worldwide leader in plastic tableware."})
  
  (let [{category :category, :or {category "Category not found"}} client]
    (str category))
  => 
  Error: 'category' not found 
  ```
- I bet it's the same problem as before... fixed it
- In this case, I need to do this:
- ```clojure
  (def b '{category :category, :or {category "Category not found"}})
  (def defaults (:or b))
  (get defaults 'category)
  => "Category not found" 
  ```
- When I should be able to do `(defaults 'category)`
- Symbol keys aren't working:
- ```clojure
  (def symbol-keys {'first-name "Jane" 'last-name "Doe"})
  
  (let [{:syms [first-name last-name]} symbol-keys]
    (str first-name last-name))
  => "" 
  ```
- In this case it's because symbol needs to optionally take a ns, like keyword
- First time that over 150 tests are passing! Current fails:
- ['binary', 'reversi', 'f', 'minesweeper', 'diamond', 'nth_prime', 'powerset', 'atbash_cipher', 'lt', 'wordy', 'ps', 'poker', 'meetup', 'spiral_matrix', 'sh', 'lazy', 'crypto_square', 'change', 'go_counting', 'dominoes', 'my_merge_with', 'cards', 'my_group_by', 'graph', 'all_your_base', 'word_sort', 'happy', 'veitch', 'allergies', 'intervals', 'queen_attack', 'seq_prons']
- What should I do for `seq?`? Obv we don't actually have ISeq so not sure what would make sense.
- looks like I might just alias it to `list?`
- find-path
- ```clojure
  (defn solutions [n]
    (concat [(* n 2) (+ n 2)]
      (if (even? n) [(/ n 2)] [])))
  
  (defn find-path [s e]
    (loop [opts [s] depth 1]
      (if (some #{e} opts)
        depth
          (recur (mapcat solutions opts) (inc depth)))))
  ```
- Added `map-indexed`, but it revealed another problem with the lambda shorthand.
- `(map-indexed #(when (= 2 %2) [%1 "Hi"]) [1 1 2 2])` fails but
- `(map-indexed (fn [x y] (when (= 2 y) [x "Hi"])) [1 1 2 2])` works
- it has something to do with the order of the args. I guess I need to sort them?
- I did it, and also learned that js `sort` takes an optional compare fn:
- ```js
  var arg_strs = Array.from(new Set(ast.toString().match(/%\d?/g)))
  var sort_strs = arg_strs.sort((a, b) => parseInt(a.substring(1)) - parseInt(b.substring(1)))
  var args = sort_strs.map(x => types._symbol(x))
  return types._function(EVAL, Env, a1, env, args);
  ```
- This means I can make sort-by!
- If the keyfn is `count`, we need to produce a function like
- ```clojure
  (fn [a b] (- (count a) (count b)))
  ```
- Nice!
- ```clojure
  (sort-by count ["aaa" "bb" "c"])
  => ["c" "bb" "aaa"] 
  ```
- I added a special case for sorting by map keys.
- Even this crazy example works:
- ```clojure
  (def x [{:foo 2 :bar 11}
   {:bar 99 :foo 1}
   {:bar 55 :foo 2}
   {:foo 1 :bar 77}])
  ; sort-by given key order (:bar)
  (def order [55 77 99 11])
  
  (sort-by 
    #((into {} (map-indexed (fn [i e] [e i]) order)) (:bar %)) 
    x)
  => [{:bar 55 :foo 2} {:foo 1 :bar 77} {:bar 99 :foo 1} {:foo 2 :bar 11}] 
  ```
- This one, however, is wrong:
- ```clojure
  (def x [{:foo 2 :bar 11}
          {:bar 99 :foo 1}
          {:bar 55 :foo 2}
          {:foo 1 :bar 77}])
  
  ;sort by :foo, and where :foo is equal, sort by :bar
  (sort-by (juxt :foo :bar) x)
  => [{:foo 2 :bar 11} {:bar 99 :foo 1} {:bar 55 :foo 2} {:foo 1 :bar 77}] 
  ;; should be
  ;; ({:foo 1, :bar 77} {:bar 99, :foo 1} {:foo 2, :bar 11} {:bar 55, :foo 2})
  ```
- I'm guessing it's because juxt returns a vector and it's supposed to know to use the second element if the firsts are equal? IDK
- Yes, there is a thing to compare indexed collections:
- ```clojure
  (defn ^:private compare-indexed
    "Compare indexed collection."
    ([xs ys]
       (let [xl (count xs)
             yl (count ys)]
         (cond
          (< xl yl) -1
          (> xl yl) 1
          (== xl 0) 0
          :else (compare-indexed xs ys xl 0))))
    ([xs ys len n]
       (let [d (compare (nth xs n) (nth ys n))]
         (if (and (zero? d) (< (+ n 1) len))
           (recur xs ys len (inc n))
           d))))
  ```
- It also doesn't take an optional comparator, though I suppose I could do that. I did it, but some things are still not working, like this:
- ```clojure
  (sort-by val > {:foo 7, :bar 3, :baz 5})
  => {:foo 7 :bar 3 :baz 5} 
  ```
- I think the key is in the way compare functions are suppose to work:
- > `compareFn`: A function that defines the sort order. The return value should be a number whose sign indicates the relative order of the two elements: negative if a is less than b, positive if a is greater than b, and zero if they are equal. NaN is treated as 0. The function is called with the following arguments...
- That's why if I change `-` to `+`, it will not sort them, because then all the values will be positive. But if it's something like `#(> %2 %1)`
- omg I actually did it
- ```clojure
  (sort-by val > {:foo 7, :bar 3, :baz 5})
  => {:foo 7 :baz 5 :bar 3} 
  ```
- The trick was to port `fn->comparator` from Clojurescript:
- ```js
  function makeComparator(f) {
      return function(x, y) {
          let r = f(x, y)
          if (types._number_Q(r)) {
              return r
          } else if (r) {
              return -1
          } else if (f(y, x)) {
              return 1
          } else {
              return 0
          }
      }
  }
  ```
- ## Destructuring functions
	- The `maybe-destructured` function appears to work:
	- ```clojure
	  (maybe-destructured 
	    '(defn power [[exponent bit]]
	       (if (= "1" bit)
	           (pow 2 exponent)
	           0)))
	  => 
	  ([defn power p__9 p__11] 
	   (let [[[exponent bit]] p__9 (if (= "1" bit) (pow 2 exponent) 0) p__11])) 
	  ```
	- By "work", all I can confidently say is that it produced output... I'm trying to figure out whether that's the *right* output.
	- We need to see how it's called and dealt with afterwards, which is done in the `fn` macro. But Clojure's `fn` macro also deals with pre/post conditions and named anonymous functions, so it could possibly be simplified for our purpose. Of course, we do need the name in the case of `defn`...
	- `maybe-destructured` is only supposed to receive the params and the body:
	- ```clojure
	  (maybe-destructured 
	    '([[exponent bit]]
	       (if (= "1" bit)
	           (pow 2 exponent)
	           0)))
	  => 
	  ([p__13 p__15] 
	   (let [[[exponent bit]] p__13 
	         (if (= "1" bit) 
	             (pow 2 exponent)
	             0)
	         p__15]))
	  ```
	- Cool, so that's pretty much what I'd naively do, create an implicit let binding for the destructured params. But why does it need to modify the body? Actually it doesn't, it includes the body within the bindings... is that what it's supposed to do?
	- The function above expands to:
	- ```clojure
	  (def power 
	    (fn ([[exponent bit]] 
	         (if (= "1" bit) 
	           (pow 2 exponent) 
	           0))))
	  ```
	- Here is an example call:
	- ```clojure
	  ((fn ([[exponent bit]]
	         (if (= "1" bit)
	           (Math/pow 2 exponent)
	           0)))
	   [0 "1"])
	  ```
	- I think I got it all working!
	- Currently failing:
	- ['run_length_encoding', 'f', 'graph', 'lt', 'diamond', 'powerset', 'allergies', 'change', 'dominoes', 'binary', 'two_fer', 'crypto_square', 'cards', 'atbash_cipher', 'wordy', 'queen_attack', 'reversi', 'intervals', 'nth_prime', 'happy', 'lazy', 'ps', 'beer_song', 'poker', 'seq_prons', 'luhn', 'spiral_matrix', 'go_counting', 'my_group_by', 'ss', 'minesweeper', 'sh', 'word_sort', 'meetup', 'veitch', 'all_your_base', 'my_merge_with']
	- Why on earth is `two-fer` failing?
	- ```clojure
	  (defn two-fer [name]
	    (if name
	      (str "One for " name ", one for me.")
	      "One for you, one for me."))
	  
	  (two-fer)
	  => 
	  Error: Cannot read properties of undefined (reading '1') 
	  ```
	- Wow... I think that's actually correct. Because name is a function at that point. If it's not passed in... you cannot get the [1] element from it!
	- Clojure gives an arity exception because that is illegal!
	- Wow! It works if we use a proper multiarity function!