- ## Iterate
	- This works similar to `cycle`:
	- ```clojure
	  (defn iterate
	    "Returns a lazy sequence of x, (f x), (f (f x)) etc. f must be free of side-effects"
	    {:added "1.0"
	     :static true}
	    [f x] (clojure.lang.Iterate/create f x) )
	  ```
- # Exercises
	- Let's make a comprehensive list of the exercises and the functions still needed for the example solutions. Just the failing ones. I suppose we'll go in this arbitrary order that they appear in my console:
	- (58) ['queen_attack', 'sum_of_multiples', 'pascals_triangle', 'grade_school', 'gigasecond', 'all_your_base', 'etl', 'trinary', 'allergies', 'kindergarten_garden', 'change', 'run_length_encoding', 'rotational_cipher', 'sieve', 'rna_transcription', 'strain', 'isogram', 'prime_factors', 'bank_account', 'isbn_verifier', 'luhn', 'go_counting', 'beer_song', 'nucleotide_count', 'clock', 'yacht', 'matching_brackets', 'armstrong_numbers', 'flatten_array', 'minesweeper', 'collatz_conjecture', 'dominoes', 'binary', 'proverb', 'hamming', 'robot_simulator', 'perfect_numbers', 'pov', 'largest_series_product', 'secret_handshake', 'poker', 'hexadecimal', 'phone_number', 'crypto_square', 'meetup', 'pangram', 'binary_search_tree', 'space_age', 'sublist', 'atbash_cipher', 'say', 'scrabble_score', 'pig_latin', 'protein_translation', 'wordy', 'diamond', 'spiral_matrix', 'nth_prime']
	- queen_attack: assoc-in, cond->
	- sum_of_multiples: cond->
	- pascals_triangle: partition-all, lazy-seq, iterate
	- grade_school: sorted-map
	- gigasecond: ?
	- all_your_base: not-every?
	- etl: for
	- trinary: .compareTo
	- allergies: bit-shift-right, bit-and, keep-indexed
	- kindergarten_garden: split-lines, interleave, zipmap
	- change: ?
	- run_length_encoding: ?
	- rotational_cipher: isUpperCase
	- sieve: cycle
	- rna_transcription: ?
	- strain': ?
	- 'isogram': ?
	- 'prime_factors': ?
	- 'bank_account': ?
	- 'isbn_verifier': ?
	- 'luhn': iterate, take-while, cycle
	- 'go_counting': case, comp, letfn, mapcat, iterate, drop-while
	- 'beer_song': case, format
	- 'nucleotide_count': zipmap
	- 'clock': format
	- 'yacht': contains?
	- 'matching_brackets': peek, pop, keys
	- 'armstrong_numbers': ?
	- 'flatten_array': tree-seq
	- 'minesweeper': split-lines
	- 'collatz_conjecture': take-while
	- 'dominoes': for, split-at
	- 'binary': map-indexed
	- 'proverb': format
	- 'hamming': map (on multiple collections)
	- 'robot_simulator': cycle, drop-while
	- 'perfect_numbers': for
	- 'pov': zippers (done, but not yet compatible)
	- 'largest_series_product': max, Character/digit
	- 'secret_handshake': Integer/toBinaryString
	- 'poker': group-by, vals, condp, for, juxt, sorted-map
	- 'hexadecimal': ?
	- 'phone_number': re-find
	- 'crypto_square': Math/ceil
	- 'meetup': get-in, flatten
	- 'pangram': compare, Character/toLowerCase, distinct
	- 'binary_search_tree': ?
	- 'space_age': intern
	- 'sublist': partition
	- 'atbash_cipher': interleave
	- 'say': ??????
	- 'scrabble_score': reduce-kv, zipmap, merge, .toUpperCase
	- 'pig_latin': str/split
	- 'protein_translation': case, take-while
	- 'wordy': re-matches,, keys, partition-all
	- 'diamond',: iterate
	- 'spiral_matrix': ?
	- 'nth_prime: iterate, for
- Guess what *none* of these exercises need? namespaces!
- All right... you talked me into it, lol, I'll compile a full list:
- 1. re-matches
  2. re-find
  3. str/split
  4. comp
  5. contains?
  6. zipmap
  7. format
  8. not-every?
  9. merge
  10. reduce-kv 
  11. interleave
  12. intern
  13. distinct
  14. keep-indexed
  15. compare
  16. flatten
  17. cycle
  18. split-lines
  19. mapcat
  20. letfn
  21. sorted-map 
  22. juxt
  23. for
  24. condp
  25. bit-and
  26. group-by 
  27. Integer/toBinaryString
  28. Character/digit
  30. map (on multiple collections)
  31. map-indexed, 
  32. split-at,
  33. tree-seq
  34. bit-shift-right
  35. case
  36. take-while
  37. drop-while
  38. iterate
  39. zipmap
  40. cond->
  41. .compareTo
  42. partition-all
  43. lazy-seq
  44. sorted-map
- Somehow we lost complex numbers. 19 exercises passing:
- ['roman_numerals', 'triangle', 'bob', 'difference_of_squares', 'raindrops', 'grains', 'hello_world', 'acronym', 'octal', 'robot_name', 'zipper', 'series', 'reverse_string', 'word_count', 'two_fer', 'anagram', 'leap', 'accumulate', 'binary_search']
- uh... now it's passing again... what did I do?
- ['word_count', 'difference_of_squares', 'anagram', 'accumulate', 'roman_numerals', 'complex_numbers', 'binary_search', 'hello_world', 'bob', 'leap', 'octal', 'raindrops', 'reverse_string', 'triangle', 'two_fer', 'series', 'grains', 'acronym', 'robot_name']
- ['anagram', 'octal', 'grains', 'armstrong_numbers', 'triangle', 'hello_world', 'roman_numerals', 'two_fer', 'robot_name', 'series', 'word_count', 'accumulate']
- Nearly finished integrating immutable hashmaps, but having problems evaluating nested maps.
- I need to fix the clause in `eval_ast`:
- ```js
  else if (types._hash_map_Q(ast)) {
      ast = ast.toJS()
      console.log(ast)
      var new_hm = {};
      for (const k in ast) {
        new_hm[k] = EVAL(ast[k], env);
      }
      return new Map(Seq(new_hm));
    }
  ```
- I should be using `update` I think.
- I think I've got it, but there's a problem with the way it's printing the commas. That part is a mess anyway.
- I disabled that for now.
- Now it's doing something weird...
- ```clojure
  (def mymap
    {:a 1})
  
  mymap => {:a 1} 
  ```
- ```clojure
  (defn mymap-fn []
    {:a 1})
  
  (mymap-fn) => 
  Error: Unknown type 'object' 
  ```
- So there's something wrong with how it's defining functions.
- In the function, it's defined as a normal object, not a map.
- The `fnBody` being passed to the function is correct.
- Indeed. A conversion is happening in types._function.
- It's something in the `swapRecur` business.
- I bet it's because of `walk`! It's not set up for the immutable maps. Is it?
- Nope:
- ```js
  else if (_hash_map_Q(form)) {
          const entries = seq(form).map(inner)
          let newMap = {}
          entries.forEach(mapEntry => {
              newMap[mapEntry[0]] = mapEntry[1]
          });
          return outer(newMap)
      }
  ```
- What is the forEach doing? Ah, it's setting the entries in the new map. Let's instead do it with the `Map` constructor.
- Oh yay, it works now!
- And it got rid of the forEach:
- ```js
  else if (_hash_map_Q(form)) {
          const entries = seq(form).map(inner)
          console.log("Walking hash-map. Entries:", entries)
          const newMap = new Map(entries)
          console.log("newMap:", newMap)
          return outer(newMap)
      }
  ```
- `zipper` is still failing, but in a different test than before:
-
- ```clojure
  (-> {:value 1
       :left  {:value 2
               :left  nil
               :right {:value 3
                       :left  nil
                       :right nil}}
       :right {:value 4
               :left  nil
               :right nil}}
    zipper/from-tree
    zipper/right) 
  => {:tree {:value 4 :left nil :right nil} 
      :trail [["right" 1 {:value 2 
                          :left nil 
                          :right {:value 3 :left nil :right nil}}] []]}
  ```
- ```clojure
  (-> {:value 1
       :left  {:value 2
               :left  nil
               :right {:value 3
                       :left  nil
                       :right nil}}
                :right {:value 4
                        :left  nil
                        :right nil}}
               zipper/from-tree
               zipper/left
               zipper/up
               zipper/right) 
  => {:tree {:value 4 :left nil :right nil} 
      :trail [["right" 1 {:value 2 
                          :left nil 
                          :right {:value 3 :left nil :right nil}}] []]}
  ```
- Ha. It's the same result, but `=` returns `false`. And we already fixed the equality function so it should be using proper value semantics.
- I printed them to the console, it's hitting the right clause and they are indeed equal. See:
- ```clojure
  (=
     {:tree {:value 4 :left nil :right nil} 
      :trail [["right" 1 {:value 2 
                          :left nil 
                          :right {:value 3 :left nil :right nil}}] []]}
  
     {:tree {:value 4 :left nil :right nil} 
      :trail [["right" 1 {:value 2 
                          :left nil 
                          :right {:value 3 :left nil :right nil}}] []]})
  => false 
  ```
- I don't know what to do. This seems like a bug. A really bad one that shouldn't exist in a library like this.
- Try to make a minimal repro, I guess...
- It only fails on deeply nested maps, of more than 2 levels. This works:
- ```clojure
  (=
     {:value 2 
      :left nil 
      :right {:value 3 :left nil :right nil}}
     {:value 2 
      :left nil 
      :right {:value 3 :left nil :right nil}})
  => true
  ```
- Wait... it's the vector that is messing it up... This still fails:
- ```clojure
  (= {:tree {} :trail []}
     {:tree {} :trail []})
  => false
  ```
- But it works if they're empty maps instead of vectors:
- ```clojure
  (= {:tree {} :trail {}}
     {:tree {} :trail {}})
  => true
  ```
- I do not like this. How do I tell if they're equal? I already tried sequencing them and it does the same thing.
- This certainly does feel like a bug. I hate it. I might have to just go back to using regular objects...
  id:: 64cf1cfd-037d-4371-9afd-3494a260722d
- Made the repro even smaller:
- ```clojure
  (= {"trail" []}
     {"trail" []})
  => false
  ```
- Now I need to make a new project, and see if it works using immutable.js directly.
- Ah! It works if I use the original code for equality of hashmaps, and just convert them to objects:
- ```javascript
  a = a.toObject()
  b = b.toObject()
  if (Object.keys(a).length !== Object.keys(b).length) { return false; }
  for (var k in a) {
      if (!_equal_Q(a[k], b[k])) { return false; }
  }
  return true;
  ```
- So now zipper is passing! I guess I can merge this now.
- # `lazy-seq`
	- The biggest reason I reached for immutable-js was for lazy sequences, but I still haven't achieved what `lazy-seq` does, and tbh I've never actually used it so I'm not very clear about what it does.
	- Everything about this interpreter is implemented as naively as possible, prioritizing ease of understanding above all else. So I might feel brave enough to attempt to implement lazy-seq in my own naive way.
	- It seems that the basic functionality is not much different from defining a function, because what is a function? A piece of code that is stored for later execution. Just in this case, the way it is evaluated is much more controlled.
	- So if we create a lazy-seq type similar to a function, our interpreter can handle it appropriately. In a sense this will amount to the creation of a kind of separate "mini-interpreter" with different execution properties.
	- [This article](https://clojure-goes-fast.com/blog/clojures-deadly-sin/) seems to confirm my point:
		- > The principles of lazy evaluation are easy to simulate in any language that supports wrapping arbitrary code into a block and giving it a name (anonymous functions, anonymous classes — they all work). In Clojure, that would be a plain lambda or a dedicated delay construct:
		- ```clojure
		  (fn [] (+ 2 3)) ;; As lazy as it gets
		  
		  (delay (+ 2 3)) ;; Similar, but the result is computed once and cached.
		  ure
		  ```
	- First, I need to understand how lazy-seq actually works, with a few simple examples:
	- ```clojure
	  (defn fib-seq
	    "Returns a lazy sequence of Fibonacci numbers"
	    ([]
	     (fib-seq 0 1))
	    ([a b]
	     (lazy-seq
	      (cons b (fib-seq b (+ a b))))))
	  
	  (take 10 (fib-seq))
	  ;;=> (1 1 2 3 5 8 13 21 34 55)
	  ```
	- Here's an example from Joy of Clojure, which demonstrates how the head and the thunk work together:
	- ```clojure
	  (defn lazy-range [i limit]
	    (lazy-seq
	      (when (< i limit)
	        (println "REALIZED")
	        (cons i (lazy-range (inc i) limit)))))
	  
	  (def first-ten (lazy-range 0 10))
	  
	  (take 2 first-ten)
	  ; REALIZED
	  ; REALIZED
	  ; (0 1)
	  ```
	- The caching part is very important. Here's a paragraph from the wiki article on lazy evaluation:
	- > Lazy evaluation is often combined with memoization, as described in Jon Bentley's Writing Efficient Programs.[4] After a function's value is computed for that parameter or set of parameters, the result is stored in a lookup table that is indexed by the values of those parameters; the next time the function is called, the table is consulted to determine whether the result for that combination of parameter values is already available. If so, the stored result is simply returned. If not, the function is evaluated, and another entry is added to the lookup table for reuse.
	- We have a memoize function, adapted from clojure (which we used in the Exercism coordinate transformation exercise):
	- ```clojure
	  (defn memoize [f]
	      (let [mem (atom {})]
	        (fn [& args]
	          (let [key (str args)]
	            (if (contains? @mem key)
	              (get @mem key)
	              (let [ret (apply f args)]
	                (do
	                  (swap! mem assoc key ret)
	                  ret)))))))
	  ```
	- The wiki article has a useful thing here: https://en.wikipedia.org/wiki/Lazy_evaluation#JavaScript
	- ```js
	  /**
	   * Generator functions return generator objects, which reify lazy evaluation.
	   * @return {!Generator<bigint>} A non-null generator of integers.
	   */
	  function* fibonacciNumbers() {
	      let memo = [1n, -1n]; // create the initial state (e.g. a vector of "negafibonacci" numbers)
	      while (true) { // repeat indefinitely
	          memo = [memo[0] + memo[1], memo[0]]; // update the state on each evaluation
	          yield memo[0]; // yield the next value and suspend execution until resumed
	      }
	  }
	  
	  let stream = fibonacciNumbers(); // create a lazy evaluated stream of numbers
	  let first10 = Array.from(new Array(10), () => stream.next().value); // evaluate only the first 10 numbers
	  console.log(first10); // the output is [0n, 1n, 1n, 2n, 3n, 5n, 8n, 13n, 21n, 34n]
	  ```
	- I'm less excited about it after learning a little bit about it, because it's not exactly necessary other than compatibility with Clojure code, which I'm not entirely sure is an absolute goal. Like, there's not an exercise that requires it, I don't think.
	- Pascal's triangle uses it. Let's check that out.
	- ```clojure
	  (defn- next-row [row]
	    (lazy-seq (->> (partition-all 2 1 row)
	                   (map (partial apply +'))
	                   (cons 1))))
	  
	  (def triangle (iterate next-row [1]))
	  
	  (defn row [n] (nth triangle (dec n)))
	  ```
	- Let's see if there's any solution that we could swap.
	- This one doesn't use it:
	- ```clojure
	  (defn- next-row [cur-row]
	    (let [a (cons 0 cur-row)
	          b (conj cur-row 0)]
	      (into [] (map +' a b))))
	  
	  (def triangle
	    (iterate next-row [1]))
	  
	  (defn row [n]
	    (last (take n triangle)))
	  ```
	- They all seem to use `iterate`.
	- Ah, here's one that doesn't:
	- ```clojure
	  (defn- row-sum [row]
	    (map (partial apply +') (partition 2 1 (concat [0] row [0]))))
	  
	  (defn row [n]
	    (if (= n 1)
	      [1]
	      (row-sum (row (- n 1)))))
	  
	  (def triangle
	    (map row (drop 1 (range))))
	  ```
	- They all use that plus prime function `+'`, what is that?
	- It's because it doesn't throw on integer overflow. It's only necessary for the following test:
	- ```clojure
	  (testing "300th row" 
	           (is (some? (some #{768408467483699505953134992026497450726137182648496343119395977464120N} 
	                            (row 300)))))
	  ```
	- For some reason the test suite I have doesn't include that one... did I remove it or something? Yep, I sure did. javascript might not be able to handle that number. Does it have like bigintegers or something?
	- This solution almost works - but reveals a problem with our equality check because it's evaluating lists and vectors with the same values as unequal:
	- ```clojure
	  (= [[1]] (take 1 triangle) => ([1]) )
	  ```
	- The important part is, we have lazy ranges so it actually works! I could just change the tests.
	- Spoke too soon... the second test fails:
	- `(take 2 triangle) => ([1] ("Seq [ 0, 1, 0 ]undefined" "Seq [ 1, 0 ]undefined"))`
	- I suspect this is a problem with `partition`, which I never completed properly.
	- Indeed, this should be:
	- ```clojure
	  (partition 2 1 (concat [0] [1] [0]))
	  => ((0 1) (1 0))
	  ```
	- But ours outputs
	- ```clojure
	  (partition 2 1 (concat [0] [1] [0]))
	  => ((0 1 0) (1 0)) 
	  ```
	- ```clojure
	  (defn partition
	    ([n coll]
	     (partition n n coll))
	    ([n step coll]
	     (lazy-seq
	      (when-let [s (seq coll)]
	        (let [p (doall (take n s))]
	          (when (= n (count p))
	            (cons p (partition n step (nthrest s step)))))))))
	  ```
	- I did it!
	- ```clojure
	  (defn partition [n step coll]
	    (if-not coll 
	      (partition n n step)
	      (loop [s coll p []]
	        (if (= 0 (count s))
	          (filter #(= n (count %)) p)
	          (recur (drop step s)
	                 (conj p (take n s)))))))
	  ```
- Weird, there's some nondeterminism happening with test runs
- ['anagram', 'acronym', 'robot_name', 'grains', 'zipper', 'hello_world', 'bob', 'octal', 'difference_of_squares', 'word_count', 'two_fer', 'roman_numerals', 'complex_numbers', 'accumulate', 'raindrops', 'binary_search', 'armstrong_numbers', 'series', 'leap', 'triangle', 'reverse_string']
- the complex numbers exercise sometimes fails.
- This could very well be the order, which is random. Because... we're not clearing the env in between exercises, so the names just pile up
- Weird... I patch some serious bugs, and... fewer exercises are passing. Lost 6, actually... it was because I changed `empty?` to something I shouldn't have but it's fixed now
- Something weird here:
- ```clojure
  (apply str (map translate "AC"))
  => "(U G)" 
  ```
- It works with lists, but if it's the result of map instead it stringifies the entire seq as if there's no `apply`.
- Yeah, we can see the same result:
- ```clojure
  (apply str (seq '("U" "G")))
  => "(U G)" 
  ```
- # `for`
	- Implementing this as a special form.
- ok this is confusing. It seems like it actually needs to be like 3 (or more) nested loops.
- 1. Loop through each binding pair and evaluate seq on the right 
  2. Loop through each value in the first seq, with each value of each seq
  3. (what, I'm so confused)
- So we have the bindings, each pair has a seq.
- God, this is annoying. I just can't seem to get my head around it.
- [[Clojure interpreter notes page 7]]
-