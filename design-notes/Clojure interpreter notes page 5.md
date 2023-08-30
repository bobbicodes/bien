- ## Immutable.js
	- I bet I could make very good use of my time by simply porting functions from the immutable.js library. It's got so much!
	- I've got lists and maps, but sets aren't working.
	- I guess the more important part is implementing all the seq methods.
	- Oh, so we don't currently handle hashmaps with integer keys. Let's do that.
	- It's creating the map, but then doing something to it that is wrong. Got to trace through everything it touches.
	- I think I got it. It was the printer.
	- It works, but need to fix its printer:
	- ```clojure
	  (hash-map ["a" 1] ["b" 2])
	  => Map { "a": 1, "b": 2 }
	  ```
	- That's as close as it wants to get... what if I turn it into a seq? Idk if I tried that
	- I got it! Came up with a solution using `interleave`:
	- ```clojure
	  case 'hash-map':
	       var ret = obj.keySeq().interleave(obj.valueSeq()).join(' ')
	       return "{" + ret + "}"
	  ```
	- The only problem is, there are no commas separating the map entries. Oh well, I'll fix it later
	- Wait, there is a slightly bigger problem, that the kvs are supposed to be passed to hash-map individually, not in pairs. So, what we need is to pass it through partition 2. I'll port more functions and I imagine it will start to take shape.
	- Did we do interleave yet? I just woke up.
	- ```clojure
	  (interleave (list [1 2 3]) (list ["a" "b" "c"]) (list ["d" "e" "f"]))
	  ```
	- I can get it working with 2 lists, but not more because I can't figure out the apply syntax... nothing is working
	- omg I got it! Here is what worked:
	- ```js
	  function interleave() {
	      var args = Array.prototype.slice.call(arguments, 1)
	      var ret = arguments[0].interleave.apply(arguments[0], args)
	      console.log("interleaving", ret)
	      return ret
	  }
	  ```
	- wow. that was annoying. but now I know how to do it. I was missing the `this` arg to apply, because that's stupid. but it won't bee torturing me anymore because I know its secrets
	- Oh, there is a `partition`. If I get that working I can use it for `hash-map` so we don't have to pass it vectors.
	- wtf I don't understand the syntax. It seems to want a predicate. what is this supposed to mean
	- ```
	  partition<F, C>(
	    predicate: (this: C, value: T, index: number, iter: this) => boolean,
	    context?: C
	  ): [List<T>, List<F>]
	  partition<C>(
	    predicate: (this: C, value: T, index: number, iter: this) => unknown,
	    context?: C
	  ): [this, this]
	  ```
	- what am I looking at? what is `F`? what is `C`? Is `partition` not what I think it is? why is this not documented better?
	- I want to call `(hash-map "a" 1 "b" 2)`.
	- omg, that was so awful but I finally got it
	- ```clojure
	  (hash-map "a" 1 "b" 2 "c" 3)
	  => {a 1 b 2 c 3}
	  ```
	- This is the stupid function:
	- ```js
	  export function _hash_map() {
	      let args = []
	      for (let i = 0; i < arguments.length; i+=2) {
	          args.push([arguments[i], arguments[i+1]])
	      }
	      return Map(args)
	  }
	  ```
	- Why was that so hard?? because I hate everything. First, I forgot the `=` in `i+=2` and couldn't understand why it was hanging the browser. Then, I thought I'd need to use `apply` but apparently not... the function takes a single list and not a series of args like I thought.
	- Print hashmaps with commas. This also was much more annoying than I wish it was
	- ```js
	  case 'hash-map':
	              let kvstring = obj.keySeq().interleave(obj.valueSeq()).join(' ')
	              let kvs = kvstring.split(' ')
	              let hmstring = ""
	              for (let i = 0; i < kvs.length; i++) {
	                  if (i % 2 === 0) {
	                      hmstring = hmstring + kvs[i] + ' '
	                  } else if (i === kvs.length-1) {
	                      hmstring = hmstring + kvs[i]
	                  } else {
	                      hmstring = hmstring + kvs[i] + ', '
	                  }
	              }
	              return "{" + hmstring + "}"
	  ```
- Wait, so there's a problem with the namespace issue that I thought I had elegantly solved in the last PR. It's because if a symbol is not found, it causes an error. So... maybe we make real namespaces?
- Oh hold on, I do have a way to check if the var exists in the env.
- So, do I really want vars called `user/whatever`? Yes, I suppose we do. That *is* how Clojure does it. Weird that I hadn't caught that yet.
- The value we print for the user even claims that:
- `"Defined: #'hello-world/hello"`
- # Namespaces
	- What I need is a function called `resolve` which contains the lookup logic, because we have to use it in several places, for every way of defining vars, and all the different types of functions.
	- I first removed the var lookup logic from the env module. It only does lookup in the different scopes.
	- So do we use resolve for *defining* the var? or just when calling it?
	- Just when calling it.
	- But, does it return a symbol or a string? Operations following it seem to want a symbol so let's try that
- Something's wrong and I'm having trouble finding it.
- Wow, I got it. It wasn't calling functions properly after resolving them.
- This is the proper call:
- `f = EVAL(resolve(ast[0].value, env), env)`
- Now it's failing on multi-arity functions because it switches the env to outer. Which means my resolve function needs to look there too, and not just in `data`...
- ok so I did that, but now there's another issue I think I realized. Our method of checking if the var is multiarity no longer works, because the name has not yet been resolved to a symbol. And we can't pass it to resolve as it is, because resolve doesn't know to look for multiarity functions! Let's fix that.
- I got a little closer but this is a hard problem, I can't expect to get it right away. I think the lookup logic in the default case of the interpreter is wonky. First we check if the function has a variadic definition, then we check for a fixed arity, otherwise call the variadic, and then look for a fixed arity again? Is that necessary? Wait... it might be actually. Because if the fn is variadic it still might match a fixed arity. I hope that makes sense later because it makes sense now.
- Idk... does Clojure actually allow that ambiguity? Can you have a certain fixed arity and a variadic arity with the same number of args? Let's try it
- ```clojure
  (defn myfn
    ([a b c] (str a b c))
    ([a & b] (str "variadic" a (apply str b))))
  
   (myfn "hello" "kitty")
  "variadichellokitty"
  ```
- ok cool. We first check if there's a matching fixed arity
- I should have the resolve function do all the lookup logic, including the arity stuff.
- `resolve` will just take the ast and the env.
- I think I've got it somewhat close. I'm happy because the code is arranged very logically.
- But it isn't successfully retrieving any vars at all. It calls eval_ast, and it doesn't recognize it as a symbol even though it is.
- ok, finally tracked down that bug.
- Only to reach a new one, of course. For some reason the env is getting lost. So at least I have some idea what the problem is... but it doesn't make sense. I checked every call site and the env is being passed every time.
- Going to take a nap. The problem is pretty obvious... it's printing the env in the console at every point... it's there... and then it isn't
- ## Well I didn't sleep much
	- I'm thinking of changing my attitude by, instead of feeling frustrated that it's not doing what it should... to try to pretend that I want that behavior, and try to understand why it "works".
	- So... I have a magically disappearing environment. How did I manage that?
	- The function is defined in the env properly, as `user/a`. Then it is called.
	- The args are passed to eval_ast, along with the current env.
	- oh my god... I see the bug:
	- ```js
	  var f = EVAL(resolve(ast, env))
	  ```
	- We're passing the env to resolve... but not to EVAL!
	- It still doesn't work. But I solved the env issue.
	- Now it's not finding the `__ast__` property on the function. I'll print it to try to see why.
	- the actual defined function works. But then it calls the function body, which is a call to `str`.
	- It seems like it's evaluating in circles.
	- This is the error I'm getting:
	- `Error: Cannot read properties of null (reading '__ast__')`
	- But how is that possible? It first checks if that property exists before trying to use it. That's how we know if it's a user defined function.
	- That's indeed the line that is reporting the error:
	- ```js
	  if (f.__ast__) {
	            console.log("setting env to function scope")
	            ast = f.__ast__;
	            env = f.__gen_env__(args);
	  ```
	- It's erroring because `f` is null.
	- `resolve` sometimes receives symbols, and sometimes a list. That's the problem I think. We need to make it so if passed
	- waaaaaat... this makes zero sense.
	- omg, I finally got it! Holy shit! That was one of the most intense debugging sessions I ever had... I was missing a return statement in one place.
	- Multi-arity functions are still not working. It prints `Looking for `two-fer/two-fer` in two-fer`, but fails to return it even though `two-fer/two-fer-arity-0` and `two-fer/two-fer-arity-1` are both in the env.
	- Because this is wrong:
	- ```js
	  if (vars[i] === namespace + "/" + varName + '-arity-' + ast.length-1) {
	  ```
	- It's wrong because it's not being called on the entire ast. It's being called on the symbol! I think the only way we can get the actual arity is if we pass it.
	- It worked! Holy shit! Something worked!
	- The gotcha at the moment is that the namespace resolution takes a ridiculous amount of time. I'm hoping it's because I put like 100 prints to the console trying to debug this.
	- Yes! I commented out all the logs and it went just as fast as before!
	- hello world and twofer are now passing, but reverse string is having an env lookup failure.
	- Ah! I got it, it was a silly mistake in the solution. This is great! I think I'll merge it!
	- ok so now there's a bit of a problem, because we're not requiring the namespaces.
	- Until we do... I might just remove the namespace prefixes from the tests.
	- I'm back to 7 exercises passing again! That was intense, these last few days with the namespace stuff.
	- What if... we just check if the function name has a slash (`/`) in it, and if so... just don't resolve it! It will only be 1 or 2 lines!
	- The `prime_factors` exercise is hanging the page
- There seems to be a problem with threading macros.
- We've tried 2 different versions of `reduce`, one in Clojure and one in javascript. The Clojure one hangs for some reason.
- Currently passing: ['roman_numerals', 'two_fer', 'robot_name', 'reverse_string', 'armstrong_numbers', 'hello_world']
- `accumulate` is failing because of the treading macro.
- I don't know when they broke, but this is what is happening:
- ```clojure
  (-> "hello" (str " kitty"))
  => (" kitty" "hello")
  ```
- oh... it's because macroexpand was disabled. But I thought I specifically checked that...
- and, it fails to load core with it enabled.
- I'm considering rolling back all the namespace stuff. It feels janky and complicated. But I won't get rid of it yet.
- Or maybe I will...
- Even this fails at `is_macro_call()`: `(do true true)`
- Wait why are we looking in the env for `do`? It's a special form, not a symbol
- Ok, the namespaces are hurting my brain. So I made 2 new branches: namespaces2 (because there is already a `namespaces`), and no-namespaces.
- ['word_count', 'armstrong_numbers', 'two_fer', 'grains', 'reverse_string', 'hello_world', 'roman_numerals', 'robot_name', 'difference_of_squares']
- 1. hello world
  2. two-fer
  3. reverse string
  4. accumulate
  5. series
  6. robot_name
  7. anagram
  8. triangle
  9. word_count
  10. armstrong_numbers
  11. difference_of_squares
  12. roman_numerals
  13. grains
- Multi-arity is broken, and I... can't seem to remember how it's supposed to work!
- cool, fixed
- There's a problem with `and`:
- ```clojure
  (and true) => 
  Error: Cannot read properties of null (reading 'length') 
  ```
- Well, I guess now I get to debug a macro
- ```clojure
  (defmacro and
    (fn [& xs]
         (cond (empty? xs)      true
               (= 1 (count xs)) (first xs)
               true             
               (let (condvar (gensym))
                 `(let (~condvar ~(first xs))
                    (if ~condvar (and ~@(rest xs)) ~condvar))))))
  ```
- I think the macro is fine... I'm pretty sure I tested it when I ported it. I think there's a bug in my calling logic.
- Actually, the macro will not even expand, `macroexpand` outputs `null`. I wonder if any other macros are broken? Actually I can try this right in the app
- This is madness. I hate it. I'm sure it worked before. But something I've done must be messing it up.
- I just made it a function
- ```clojure
  (defn and [& xs]
    (every? #(identity %) xs))
  ```
- Whatever. It works.
- I've got 15 exercises passing again: 'triangle', 'hello_world', 'bob', 'roman_numerals', 'zipper', 'two_fer', 'difference_of_squares', 'grains', 'acronym', 'word_count', 'robot_name', 'reverse_string', 'accumulate', 'anagram', 'series'
- For `bob`, I used js interop:
- ```clojure
  (defn remove-whitespace [s]
    (js-eval (str "\"" s "\"" ".replace(/\\s+/g, '')")))
  ```
- # Partition
	- I think I can write this using basic maths.
	- Observe these test cases:
	- ```clojure
	  (partition 4 6 (range 20))
	  ;;=> ((0 1 2 3) (6 7 8 9) (12 13 14 15))
	  (partition 4 3 (range 20))
	  ;;=> ((0 1 2 3) (3 4 5 6) (6 7 8 9) (9 10 11 12) (12 13 14 15) (15 16 17 18))
	  ```
	- As we can see, the nth element of each partition can be computed as a simple `range` with a `step` of `step`, which is the 2nd arg to partition (defaults to n, the first arg)
	- In other words, take the first example, `(partition 4 6 (range 20))`.
	- The first elements of each partition are  `(0 6 12)`.
	- The second of each are `(1 7 13)`, etc.
	- So as we iterate through the array of partitions, we can add the correct item to each inner-array.
	- So our first loop will go from 0 to n, the first arg, which is how many are in each partition, which corresponds to how many `range`s we will generate.
	- Each range begins with its index.
	- Nice! So here are the generated ranges for the first example:
	- ```clojure
	  (partition 4 6 (range 20))
	  ;;=> ((0 6 12 18) (1 7 13 19) (2 8 14) (3 9 15))
	  ```
	- Here is the code that produced it:
	- ```js
	  function partition() {
	      if (arguments.length === 2) {
	          const n = arguments[0]
	          const coll = arguments[1]
	          return partition(n, n, coll)
	      } else if (arguments.length === 3) {
	          const n = arguments[0]
	          const step = arguments[1]
	          const coll = arguments[2]
	          let index = 0
	          const nParts = Math.floor(coll.size / step)
	          var parts = new Array(nParts).fill([]);
	          console.log("1st elements", Range(0, coll.size, step).toArray())
	          let ranges = []
	          for (var i = 0; i < n; i++) {
	              ranges.push(Range(i, coll.size, step).toArray())
	          }
	          return ranges
	      }
	  }
	  ```
	- So now we just need to take the first element of each range, the second element of each, etc.
	- I did it! Check it out:
	- ```js
	  function partition() {
	      if (arguments.length === 2) {
	          const n = arguments[0]
	          const coll = arguments[1]
	          return partition(n, n, coll)
	      } else if (arguments.length === 3) {
	          const n = arguments[0]
	          const step = arguments[1]
	          const coll = arguments[2]
	          let index = 0
	          const nParts = Math.floor(coll.size / step)
	          let ranges = []
	          for (var i = 0; i < n; i++) {
	              ranges.push(Range(i, coll.size, step).toArray())
	          }
	          let parts = []
	          for (let i = 0; i < nParts; i++) {
	              parts.push(ranges.map(x => x[i]))
	              
	          }
	          return parts
	      }
	  }
	  ```
	- This feels really great! It's like, my first time writing what I'd consider *functional javascript*
	- hmm... this causes it to hang: `(partition 8 1 [1 2 5])`
	- Well... Clojure outputs an empty list.
	- This is the test case:
	- ```clojure
	  (deftest false-start
	      (is (= :sublist (sublist/classify [1 2 5] [0 1 2 3 1 2 5 6]))))
	  ```
	- `list1` is a `sublist` of `list2`. Here's the `classify` function:
	- ```clojure
	  (defn classify
	    "Classifies two lists based on whether coll1 is the same list, a superlist,
	    a sublist, or disjoint (unequal) from coll2."
	    [coll1 coll2]
	    (let [len1 (count coll1)
	          len2 (count coll2)]
	      (cond
	        (= coll1 coll2) :equal
	        (and (> len1 len2) (list-contains? coll1 coll2)) :superlist
	        (and (> len2 len1) (list-contains? coll2 coll1)) :sublist
	        :else :unequal)))
	  ```
	- It's not a superlist per the second condition (`(and (> len1 len2) (list-contains? coll1 coll2))`).
	- And here is an example of where `and` needs to not evaluate the second item, because `(> len1 len2)` evaluates to `false`. So that's the problem. And the `and` macro is fucked and so we're currently using a regular function which evaluates all its args.
	- So if we can't fix `and` atm, in the meantime we could just change it to an `if`, like this:
	- ```clojure
	  (if (> len1 len2)
	    (list-contains? coll1 coll2)
	    false)
	  ```
	- Complete solution:
	- ```clojure
	  (ns sublist)
	  
	  (defn- list-contains?
	    "Returns truthy when list2 is contained within list1, nil otherwise"
	    [list1 list2]
	    (some #(when (= % list2) val)
	          (partition (count list2) 1 list1)))
	  
	  (defn classify
	    "Classifies two lists based on whether coll1 is the same list, a superlist,
	    a sublist, or disjoint (unequal) from coll2."
	    [coll1 coll2]
	    (let [len1 (count coll1)
	          len2 (count coll2)]
	      (cond
	        (= coll1 coll2) :equal
	        (if (> len1 len2) (list-contains? coll1 coll2) false) :superlist
	        (if (> len2 len1) (list-contains? coll2 coll1) false) :sublist
	        :else :unequal)))
	  ```
	- Damn. It still hangs.
	- This is the offending form:
	- ```clojure
	  (def coll1 [1 2 5])
	  (def coll2 [0 1 2 3 1 2 5 6])
	  (list-contains? coll2 coll1)
	  ```
	- So the problem is actually in the `list-contains?` part:
	- ```clojure
	  (defn- list-contains?
	    "Returns truthy when list2 is contained within list1, nil otherwise"
	    [list1 list2]
	    (some #(when (= % list2) val)
	          (partition (count list2) 1 list1)))
	  
	  (def coll1 [1 2 5])
	  (def coll2 [0 1 2 3 1 2 5 6])
	  (def len1 (count coll1))
	  (def len2 (count coll2))
	  
	  (> len1 len2)
	  (> len2 len1)
	  
	  (def list1 coll2)
	  (def list2 coll1)
	  
	  (partition (count list2) 1 list1)
	  (partition 3 1 [0 1 2 3 1 2 5 6])
	  ```
	- In Clojure, it correctly returns this:
	- ```clojure
	  (partition 3 1 [0 1 2 3 1 2 5 6])
	  ;;=> ((0 1 2) (1 2 3) (2 3 1) (3 1 2) (1 2 5) (2 5 6))
	  ```
	- And ours hangs. Why is that?
	- It crashes before it even generates the ranges, in the first step:
	- ```js
	  function partition() {
	      if (arguments.length === 2) {
	          const n = arguments[0]
	          const coll = arguments[1]
	          return partition(n, n, coll)
	      } else if (arguments.length === 3) {
	          const n = arguments[0]
	          const step = arguments[1]
	          const coll = arguments[2]
	          const nParts = Math.floor(coll.size / step)
	          const ranges = Range(0, n).map(i => Range(i, coll.size, step).toArray())
	          return ranges
	          //const parts = Range(0, nParts).map(i => ranges.map(x => x[i]))
	          //return parts
	      }
	  }
	  ```
	- ```clojure
	  (partition 3 1 [0 1 2 3 1 2 5 6]) => 
	  Error: Cannot perform this action with an infinite size. 
	  ```
	- Weird, isn't it?
	- It's taking `Range(0, n)` and mapping `i => Range(i, coll.size, step).toArray()` on it.
	- Replacing it with the values, it's `Range(0, 3).map(i => Range(i, coll.size, step).toArray()`
	- Hmmm. I believe `coll` needs to be a `seq`. Or... we use `.length()`?
	- Yes! Ok! That worked!
	- But the solution still crashes. What else is wrong?
	- It's indeed wrong. Here's the output in Clojure:
	- ```clojure
	  (partition 3 1 [0 1 2 3 1 2 5 6])
	  ;;=>((0 1 2) (1 2 3) (2 3 1) (3 1 2) (1 2 5) (2 5 6))
	  ```
	- Oh, wait. Derp. I forgot to uncomment the end of the partition function!
	- Ah. The reason I had coll.size before is it was using a `Range` from immutable.js, which is an indexed seq. So let's just convert whatever the coll is to one of those and we should be in business.
	- I think I know the problem. We need to take the element at the index of the seq... not the index which we were doing because it was a `range`. See:
	- ```clojure
	  (partition 3 1 [0 1 2 3 1 2 5 6])
	  => ((0 1 2 3 4 5 6 7) (1 2 3 4 5 6 7) (2 3 4 5 6 7))
	  ```
	- So like... it doesn't even make sense to use a `range`. Err... perhaps it does, but as I mentioned above, we don't do `Range(i, seq.size, step)`, we do `Range(seq.get(i), seq.size, step)`.
	- Wait no... let's back up...
	- We're not even making the correct number of parts. I'm thinking it is `seq.size` divided by `n`, divided by `step`, floored. Let's see if that makes sense. Uh, no it doesn't.
	- `(partition 3 1 [0 1 2 3 1 2 5 6])` gets split into 6 parts.
	- omg this should be so easy. I could do it in Clojure... I'm getting confused by `map`, because I'm used to the collection being mapped on being at the end. So if I solve it in Clojure, it should be easier to see how to translate it.
	- omg my brain is so broken.
	- Let's go over this again.
	- Here are the test cases:
	- ```
	  (partition 4 6 (range 20))
	  ;;=> ((0 1 2 3) (6 7 8 9) (12 13 14 15))
	  (partition 4 3 (range 20))
	  ;;=> ((0 1 2 3) (3 4 5 6) (6 7 8 9) (9 10 11 12) (12 13 14 15) (15 16 17 18))
	  (partition 3 1 [0 1 2 3 1 2 5 6])
	  ;;=> ((0 1 2) (1 2 3) (2 3 1) (3 1 2) (1 2 5) (2 5 6))
	  ```
- There's 20 solutions passing:
- `['series', 'anagram', 'reverse_string', 'roman_numerals', 'binary_search', 'accumulate', 'robot_name', 'hello_world', 'grains', 'complex_numbers', 'word_count', 'raindrops', 'difference_of_squares', 'bob', 'two_fer', 'zipper', 'leap', 'acronym', 'octal', 'triangle']`
- 5 more than previous list:
- ` 'triangle', 'hello_world', 'bob', 'roman_numerals', 'zipper', 'two_fer', 'difference_of_squares', 'grains', 'acronym', 'word_count', 'robot_name', 'reverse_string', 'accumulate', 'anagram', 'series'`
- # `cycle`
	- I could have sworn there was a `cycle` in immutable.js, but I guess that must have been one of those other lazy seq libs. It was like, one of the core abstractions.
	- It could be that it's trivially made with Seq and repeat or something.
	- `clojure.core` is no help here, it just creates a `clojure.lang.Cycle` object from the coll passed to it. So I'll do something like that too.
	- It could be as simple as vectors, which are normal arrays, like lists, only with a marker `__is_vector__`, and we check for it wherever it matters. So cycle could be just like that, just a normal list marked `__is_cycle__`., and whatever functions need to consume cycles can see it's a cycle, and however many items are needed will be calculated via modulo or whatever. Sounds like a no brainer, but I might not have thought of it if I hadn't saw that Clojure implements a special type for it.
	- Here is `vec`:
	- ```js
	  function vec(lst) {
	      if (types._list_Q(lst)) {
	          var v = Array.prototype.slice.call(lst, 0);
	          v.__isvector__ = true;
	          return v;
	      } else {
	          return lst;
	      }
	  }
	  ```
	- This is what I've got:
	- ```js
	  function cycle(coll) {
	      var c = seq(coll)
	      c.__iscycle__ = true;
	      return c
	  }
	  ```
	- Right now, it simply returns a list:
	- ```clojure
	  (cycle [1 2 3]) => (1 2 3) 
	  ```
	- What happens if we evaluate that in Clojure? Does it error or something?
	- LOL, no it actually starts spitting infinitely!
	- I actually don't know why `vec` only creates a vector if passed a list, and returns it unchanged otherwise. Wouldn't it want to like, make it a list?
	- Let's see where `cycle` is actually used in our corpus. We should start to make a habit of that because it's weird that I haven't done that yet!
	- It's only used 3 times. In sieve, luhn and robot_simulator.
	- ```clojure
	  (defn sieve
	    "Returns a list of primes less than or equal to limit"
	    [limit]
	    (loop [current-sieve (concat [false false] (range 2 (inc limit)))
	           last-prime 1]
	      (let [current-prime (->> current-sieve
	                               (drop (inc last-prime))
	                               (some identity))]
	        (if current-prime
	          (recur (map #(and %1 %2)
	                      (concat (repeat (inc current-prime) true)
	                              (cycle (concat (repeat (dec current-prime) true)
	                                             [false])))
	                      current-sieve)
	                 current-prime)
	          (filter identity current-sieve)))))
	  ```
	- ```clojure
	  (defn to-reversed-digits
	    "returns a lazy sequence of least to most significant digits of n"
	    [n]
	    (->> [n 0]
	         (iterate (fn [[i _]] [(quot i 10) (mod i 10)]))
	         (take-while (complement #{[0 0]}))
	         (map second)
	         rest))
	  
	  (defn checksum
	    "returns the luhn checksum of n, assuming it has a check digit"
	    [n]
	    (-> (->> n
	             to-reversed-digits
	             (map * (cycle [1 2]))
	             (map #(if (>= % 10) (- % 9) %))
	             (apply +))
	        (mod 10)))
	  ```
	- ```clojure
	  (def directions [:north :east :south :west])
	  
	  (defn robot [coordinates bearing]
	    {:coordinates coordinates :bearing bearing})
	  
	  (defn turn [bearing direction-list]
	    (let [dir-stream (drop-while #(not (= bearing %1)) (cycle direction-list))]
	      (nth dir-stream 1)))
	  ```
	- The `sieve` implementation is the hardest to understand. There isn't an obvious point where the cycle is realized, like `take` or something.
	- I should find as many examples as possible. From ClojureDocs:
	- ```clojure
	  (take 5 (cycle ["a" "b"]))
	  ("a" "b" "a" "b" "a")
	  ```
	- ```clojure
	  ;; Typically map works through its set of collections
	  ;; until any one of the collections is consumed.
	  ;; 'cycle' can be used to repeat the shorter collections
	  ;; until the longest collection is consumed.
	  (mapv #(vector %2 %1) (cycle [1 2 3 4]) [:a :b :c :d :e :f :g :h :i :j :k :l])
	  ;;=> [[:a 1] [:b 2] [:c 3] [:d 4] [:e 1] [:f 2] [:g 3] [:h 4] [:i 1] [:j 2] [:k 3] [:l 4]]
	  ```
	- Speaking of which, we need to make `map` work on multiple collections. `hamming` is a good example of that.
- Check it out (~2 week checkpoint):
- ![image.png](../assets/image_1691177917949_0.png)
- [[Clojure interpreter notes page 6]]