- Building out the testing framework.
- It's really a joy now with this app I made! I can pull up any exercise instantly!
- So now when an exercise is loaded, its test suite is evaluated and each `deftest` is stored in a global array called `deftests`.
- Let's look at some examples:
- ```js
  ["hello-world-test", ["is", ["=", "Hello, World!", ["hello-world/hello"]]]]
  
  [['two-fer-test', ['is', ["=", "One for you, one for me.", ["two-fer/two-fer"]]]]
   ['name-alice-test' ['is', ["=", "One for Alice, one for me.", ["two-fer/two-fer", "Alice"]]]] 
   ['name-bob-test', ['is', ["=", "One for Alice, one for me.", ["two-fer/two-fer", "Bob"]]]]]
  ```
- How about one with multiple assertions?
- Wait... If I just have `is` evaluate its ast and store the result... but first we need to strip the namespace off the var..
- I think I solved that - it simply ignores the namespace part when looking up in env.
- So when the run tests button is pressed we need to start a new env, evaluate the cell, and evaluate the tests in the same env.
- When it runs the tests it outputs `deftests` as an empty array. It's supposed to have tests in it.
- The env isn't changing.
- OK I figured out what the problem was. I had an extra return statement in deftest
- Got it! And I made a `<span>` that shows the results.
- What I need to do now is make it show which tests failed, and I need to back up because atm it's throwing away that info
- Done! Wow, it actually turned into a good day.
- hmm, but what's up with this?
- ![armstrong-bug.gif](../assets/armstrong-bug_1690052582838_0.gif)
- ah, it's because there are `testing` forms. So it isn't evaluating the `is`
- Fixed it!
- Made regular expressions. That was super easy, I added a check to the `dispatch` special form which I anticipated before, otherwise I'd have just made it "Lambda" or something. So this was easy:
- ```js
  case "dispatch":
          if (types._string_Q(a1)) {
            const re = new RegExp(a1)
            return re
          }
          let fun = [types._symbol('fn')]
          const args = ast.toString().match(/%\d?/g).map(types._symbol)
          let body = ast.slice(1)[0]
          fun.push(args)
          fun.push(body)
          return types._function(EVAL, Env, body, env, args);
  ```
- ![image.png](../assets/image_1690068194381_0.png)
- I did this because I was doing acronym
- Having trouble because it needs to be global.
- Let's see how they do it in Clojurescript
- ```clojure
  (defn- re-seq* [re s]
    (when-some [matches (.exec re s)]
      (let [match-str (aget matches 0)
            match-vals (if (== (.-length matches) 1)
                         match-str
                         (vec matches))]
        (cons match-vals
              (lazy-seq
               (let [post-idx (+ (.-index matches)
                                 (max 1 (.-length match-str)))]
                 (when (<= post-idx (.-length s))
                   (re-seq* re (subs s post-idx)))))))))
  
  (defn re-seq
    "Returns a lazy sequence of successive matches of re in s."
    [re s]
    (if (string? s)
      (re-seq* re s)
      (throw (js/TypeError. "re-seq must match against a string."))))
  ```
- I solved Acronym, but had to make a small modification:
- ```clojure
  (ns acronym)
  
  (defn acronym [text]
    (if (= text "") ""
    (->> (re-seq #"[A-Z]+[a-z]*|[a-z]+" text)
         (map first)
         (apply str)
         str/upper-case)))
  ```
- Here is `if-let`:
- ```clojure
  (defmacro if-let
    "bindings => binding-form test
  
    If test is true, evaluates then with binding-form bound to the value of 
    test, if not, yields else"
    {:added "1.0"}
    ([bindings then]
     `(if-let ~bindings ~then nil))
    ([bindings then else & oldform]
     (assert-args
       (vector? bindings) "a vector for its binding"
       (nil? oldform) "1 or 2 forms after binding vector"
       (= 2 (count bindings)) "exactly 2 forms in binding vector")
     (let [form (bindings 0) tst (bindings 1)]
       `(let [temp# ~tst]
          (if temp#
            (let [~form temp#]
              ~then)
            ~else)))))
  ```
- Which uses:
- ```clojure
  (defmacro ^{:private true} assert-args
    [& pairs]
    `(do (when-not ~(first pairs)
           (throw (IllegalArgumentException.
                    (str (first ~'&form) " requires " ~(second pairs) " in " ~'*ns* ":" (:line (meta ~'&form))))))
       ~(let [more (nnext pairs)]
          (when more
            (list* `assert-args more)))))
  ```
- Hit regex stuff. Best inspiration here is the [Clojurescript source](https://github.com/clojure/clojurescript/blob/e7cdc70d0371a26e07e394ea9cd72d5c43e5e363/src/main/cljs/cljs/core.cljs#L10216)
- It begins with `re-matches`:
- ```clojure
  (defn re-matches
    "Returns the result of (re-find re s) if re fully matches s."
    [re s]
    (if (string? s)
      (let [matches (.exec re s)]
        (when (and (not (nil? matches))
                   (= (aget matches 0) s))
          (if (== (count ^array matches) 1)
            (aget matches 0)
            (vec matches))))
      (throw (js/TypeError. "re-matches must match against a string."))))
  ```
- # Destructure
	- Check out SCI: https://github.com/babashka/sci/blob/master/src/sci/impl/destructure.cljc
	- Ha. Now I know where those random hashes were coming from in the representer when macroexpanding code.
	- There's obviously no point in starting this until we have `loop`. Regular `loop`, that is
	- Ok cool, so now that loop is basically done, I can start this! I wonder what we'll have to build on the way...
- ## Partition
	- This should be pretty easy, actually. Source:
	- ```clojure
	  (defn partition
	    "Returns a lazy sequence of lists of n items each, at offsets step
	    apart. If step is not supplied, defaults to n, i.e. the partitions
	    do not overlap. If a pad collection is supplied, use its elements as
	    necessary to complete last partition upto n items. In case there are
	    not enough padding elements, return a partition with less than n items."
	    {:added "1.0"
	     :static true}
	    ([n coll]
	       (partition n n coll))
	    ([n step coll]
	       (lazy-seq
	         (when-let [s (seq coll)]
	           (let [p (doall (take n s))]
	             (when (= n (count p))
	               (cons p (partition n step (nthrest s step))))))))
	    ([n step pad coll]
	       (lazy-seq
	         (when-let [s (seq coll)]
	           (let [p (doall (take n s))]
	             (if (= n (count p))
	               (cons p (partition n step pad (nthrest s step)))
	               (list (take n (concat p pad)))))))))
	  ```
	- It's recursive. We need `take`.
	- Could kind of use multiarity functions sometime soon.
- # Multi-arity functions
	- The best approach seems, really, to do a more bottom-up approach to get all the pieces in place so we can use Clojure core functions as they are. And this is a high priority.
	- Need to modify interpretation of `defn`.
	- Also needs to support docstrings, but I'll worry about that later.
	- I'm thinking we want to store each arity as a separate function... or maybe not?
	- It would be cool to somehow re-write it into a js function that dispatches on arg length. But I actually can't think of how I'd do that. The only way I can think of is to handle it in the interpreter.
	- First we create list of fn bodies, one for each arity.
	- Then, define each arity as a separate function:
	- ```js
	  const fnName = a1 + "-arity-" + i
	  ```
	- That much works. But then when I run
	- ```js
	  env.set(fnName, fn)
	  ```
	- The env returned is empty.
	- Ah ok it's because `env.set` needs to be passed a symbol.
	- I think I got it! So now I just need to make it work for regular functions again...
	- I completely disabled the previous behavior in order to get it working, so now it's treating every defn as multiarity.
	- I've got it worked out but it's still not working. So close though.
	- I'm not sure if the mistake is in the definition or the call. But regular function calls work, so I guess that settles it...
	- It gets the right answer... but then it tries to evaluate the answer!
	- Hmm. Well I figured that out but now I've got a weirder problem....
	- The multi-arity function works when evaluated, but not in my test runner thingy. Have to see why that would be...
	- I got `Error: 'two-fer' not found`.
	- This is the code:
	- ```clojure
	  (ns two-fer)
	  
	  (defn two-fer
	    ([] (str "One for you, one for me."))
	    ([name] (str "One for " name ", one for me.")))
	  
	  ```
- Oh, and yes, the previous solution still works.
- Why is it looking for `two-fer` and not the respective arities, like when we call it normally?
- It's not a namespace issue, I just tried it without the declaration.
- Got it! I needed to duplicate the logic that strips the namespace from the function name. Since it does its own check to see if it's a multi-arity function, and it still had the namespace!
- So I think the last thing I need to do before merging this is to get loop working again.
- This might be tricky because it needs to know where the args are
- Loop/recur works. So that's great! No functionality was lost. The only question is if you have a loop/recur inside a multi-arity fn, we might need to handle that special. But for now... I think I'm good to merge!
- # `loop`
	- I know this will probably be so easy, after I take way too long figuring it out.
	- We could start by making `partition`, because we'll want it anyway
	- Here's a little test function:
	- ```clojure
	  (loop [a 3 b []]
	    (if (= a 0)
	        b
	        (recur (dec a) (conj b a))))
	  [2 1 0]
	  ```
	- So right now I've got `loop` behaving just like `let`. But I need to implement `recur`.
	- ok I'm trying to wrap my head around this because it's so close but it's not right.
		- 1. `loop` creates a new env called `loop_env` and initializes the bindings in it. It also saves the bindings in an array called `loopvars`
		  2. but it's already wrong! Somehow, when it creates the new env, the loop variables are already defined in it???? how can that be???
	- I'm totally confused and I don't like this.
	- I finally found a way to make it work. Here's another one:
	- ```clojure
	  (loop [iter 1
	         acc  0]
	    (if (> iter 10)
	      (println acc)
	      (recur (inc iter) (+ acc iter))))
	  "55"
	  ```
	- Now this solution to `accumulate` works:
	- ```clojure
	  (ns accumulate)
	  
	  (defn accumulate [f xs]
	    (loop [xs xs
	           accum []]
	      (if
	       (empty? xs) accum
	       (recur (rest xs) (conj accum (f (first xs)))))))
	  ```
- ```clojure
  (defn fact [x]
    (loop [n x prod 1]
      (if (= 1 n)
        prod
        (recur (dec n) (* prod n)))))
  
  (fact 12) => 479001600 
  ```
- Oh, but what about recur without loop?
- whatever passing clause needs to set the loop_env, loopVars, and the loopAST.
- `loop_env`: This has each of the loop variables set in it.
- `loopVars`: in function mode (not in a `loop`) this is simply the args.
- `loopAST`: the function body. See, this isn't hard
- but... where does it get the initial values from? That needs to come from the calling function, not when we're processing the defn or fn or whatever.
- So I think that's where we need to set the vars in the loop_env. When we have access to the initial values. In other words, in the `default` case at the bottom of the interpreter.
- This is the code (in the `loop` case) that sets the loop vars:
- ```js
  for (var i = 0; i < a1.length; i += 2) {
            loop_env.set(a1[i], EVAL(a1[i + 1], loop_env))
            loopVars.push(a1[i])
          }
  ```
- What is `a1`? the vector of bindings. We traverse them in pairs.
- But here, the args passed in are `el.slice(1)`.
- Wait... this isn't going to work... because it will overwrite the variables when another function is called... what I need is to solve it like I did before, and find some way to know if we're in a loop so we know not to replace `recur`.
- hmm, this is a mind bender. Before, I did it by modifying the actual function when it is defined. But we can't do that... the `recur` needs to be preserved.
- ok I figured it out. We have that function that walks the ast and swaps the `recur` with the function itself. All we need to do it have it *not* do it if it has a `loop` in it! That will be easy enough.
- The last thing I need is recur in a `fn`:
- ```clojure
  (defn total-of [numbers]
      ((fn [func elements value]
      (if (empty? elements)
        value
        (recur func (rest elements) (func value (first elements)))))
       + numbers 0))
    
  (total-of [1 2 3])
  ```
- I can't see what the problem is, the ast looks right with this:
- ```js
  export function _function(Eval, Env, ast, env, params) {
      console.log("fn AST:", ast)
      var fn = function () {
          return Eval(ast, new Env(env, params, arguments))
      }
      let swapRecur = postwalk(x => {
          if (x.value == _symbol("recur")) {
              return fn
          } else {
              return x
          }
          return x
      }, ast)
      if (!hasLoop(ast)) {
          ast = swapRecur
        }
        console.log("fn AST:", ast)
      fn.__meta__ = null;
      fn.__ast__ = ast;
      fn.__gen_env__ = function (args) { return new Env(env, params, args); };
      fn._ismacro_ = false;
      return fn;
  }
  ```
- The weird part is it works with defn:
- ```clojure
  (defn compute-across [func elements value]
      (if (empty? elements)
        value
        (recur func (rest elements) (func value (first elements)))))
  
  (defn total-of [numbers]
      (compute-across + numbers 0))
  
  (total-of [1 2 3])
  => 6
  ```
- Wait... this works:
- ```clojure
  ((fn [func elements value]
      (if (empty? elements)
        value
        (recur func (rest elements) (func value (first elements)))))
       + [1 2 3] 0)
  => 6
  ```
- But this doesn't:
- ```clojure
  (defn total-of [numbers]
      ((fn [func elements value]
      (if (empty? elements)
        value
        (recur func (rest elements) (func value (first elements)))))
       + numbers 0))
    
  (total-of [1 2 3])
  => 
  Error: lst.slice is not a function
  ```
- So this is a big clue! But what could it mean?
- My guess is that in the case with the `defn`, it's replacing the `recur` with... the wrong function, i.e. the outer one. But I'm not quite wrapping my head around it.
- This problem merits an issue...
- # Sets
	- So the time has come to learn how the reader works! I can't believe I've avoided it until now.
	- Say we input a test expression, `#{"a" 1}`.
	- First `read_str` calls `read_form` passing it a new Reader which is passed its tokens `['#', '{', '"a"', '1', '}']`
	- Got it done, but I had to modify the reader, including the tokenizer to recognize `#{` as an opening token.
- Continue this in [[Clojure interpreter notes page 2]]
-
-