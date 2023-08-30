- Alright, so how do I want to do this?
- The way that I did it before was by having a bunch of logic inside the `defn` special form, which would analyze the shape of the definition, and if it had multiple bodies it would define each one using the pattern <name>-arity-n or <name>-variadic. Then when functions are called it would have to detect this and call the right one. It was rather problematic, and ultimately misguided because `defn` should really be a macro, an alias for `def` `fn`, with the addition of docstring metadata.
- But the real kicker is when I discovered that anonymous functions can have multiple bodies! So that will be our starting point. We'll take the `juxt` function, which is the one that I noticed has that feature. I wonder what others do?
- partial, fnil, completing, keep, nary-inline, cat, partial. At least those are the ones I could find with a simple search.
- ```clojure
  (defn juxt 
    "Takes a set of functions and returns a fn that is the juxtaposition
    of those fns.  The returned fn takes a variable number of args, and
    returns a vector containing the result of applying each fn to the
    args (left-to-right).
    ((juxt a b c) x) => [(a x) (b x) (c x)]"
    {:added "1.1"
     :static true}
    ([f] 
       (fn
         ([] [(f)])
         ([x] [(f x)])
         ([x y] [(f x y)])
         ([x y z] [(f x y z)])
         ([x y z & args] [(apply f x y z args)])))
    ([f g] 
       (fn
         ([] [(f) (g)])
         ([x] [(f x) (g x)])
         ([x y] [(f x y) (g x y)])
         ([x y z] [(f x y z) (g x y z)])
         ([x y z & args] [(apply f x y z args) (apply g x y z args)])))
    ([f g h] 
       (fn
         ([] [(f) (g) (h)])
         ([x] [(f x) (g x) (h x)])
         ([x y] [(f x y) (g x y) (h x y)])
         ([x y z] [(f x y z) (g x y z) (h x y z)])
         ([x y z & args] [(apply f x y z args) (apply g x y z args) (apply h x y z args)])))
    ([f g h & fs]
       (let [fs (list* f g h fs)]
         (fn
           ([] (reduce1 #(conj %1 (%2)) [] fs))
           ([x] (reduce1 #(conj %1 (%2 x)) [] fs))
           ([x y] (reduce1 #(conj %1 (%2 x y)) [] fs))
           ([x y z] (reduce1 #(conj %1 (%2 x y z)) [] fs))
           ([x y z & args] (reduce1 #(conj %1 (apply %2 x y z args)) [] fs))))))
  ```
- I'll start with the most basic example I can think of:
- ```clojure
  (defn a
    ([]
    (fn
      ([] "no args")
      ([n] (str "one arg: " n))))
    ([n]
     (fn
      ([] "no args")
      ([n] (str "one arg: " n)))))
  
  ((a))
  ((a) "hi")
  ```
- And we could also do `((a "what"))` or `((a "what") "hi")`
- It's hard to imagine how this would be used, so perhaps it would be better to go right to `juxt`, and maybe trim it down to make it shorter
- This only uses the 2-arity:
- ```clojure
  ((juxt :a :b) {:a 1 :b 2 :c 3 :d 4})
  ```
- This is enough for that to work:
- ```clojure
  (defn juxt
    ([f g]
     (fn
       ([] [(f) (g)])
       ([x] [(f x) (g x)])
       ([x y] [(f x y) (g x y)])
       ([x y z] [(f x y z) (g x y z)])
       ([x y z & args] [(apply f x y z args) (apply g x y z args)]))))
  ```
- I think the easiest thing would be to put a clause in the `fn` special form that checks if the arg is a list, and handles that specially.
- Here's an example where `juxt` is given 4 args, and returns a function that takes 3 args:
- ```clojure
  ((juxt + * min max) 3 4 6)
  ```
- Critically, `fn` must return a function in any case. So in order to have different modes of behavior depending on how many args it is passed, the function that it returns must know how to dispatch the correct behavior. I'm thinking I can store this as metadata on the function type.
- What that means is that we can't just call `types._function()` with the usual args for the body and params. We need to return a function that handles the args differently. I'm thinking maybe to just make a different function.
- The function that is *returned* by _function does its job by creating a new env. This is done like this:
- ```js
  new Env(env, params, arguments)
  ```
- This creates a definition in the env for each of the args passed to it. We need to find the body that has the arglist with the length corresponding to the argument count. What would be the easiest way to do this?
- I think just do a for loop that will return if it finds a match, and return an error if it doesn't find one. simple.
- ```js
  export function _multiarity_fn(Eval, Env, bodies, env) {
      var fn = function () {
          for (let i = 0; i < bodies.length; i++) {
              if (arguments.length === bodies[i][0].length) {
                  return Eval(ast, new Env(env, bodies[i][0], arguments));
              }
          }
          throw new Error("No arity defined for " + arguments.length + " args");
      }
  }
  ```
- That takes care of defining the parameters, but we still need to attach the ast and `__gen_env__`.
- This might actually make my approach slightly wrong. I'm still not so clear on what happens with the gen_env stuff at definition time vs execution time.
- If we look at the interpreter step for lambda functions:
- ```js
  if (f.__ast__) {
    ast = f.__ast__;
    env = f.__gen_env__(el.slice(1));
  }
  ```
- It's shockingly simple, really. It sets the ast to the one attached to the lambda, and sets the env by calling the function attached to it as gen_env while passing it the args.
- So if we think about it from this direction it might get clearer.
- The part I'm still kind of confused about is, when is the inner function actually called, what are the `arguments` passed there, and are they different from the args passed to gen_env?
- This is the function returned by `types._function`:
- ```js
  var fn = function () {
          return Eval(ast, new Env(env, params, arguments));
      };
  ```
- Weird... if I put a console.log in there, it isn't actually called when I run it
- Somehow I broke it:
- ```clojure
  (def hi (fn [] "hi")) => 
  Error: Cannot read properties of null (reading 'get') 
  ```
- but... defn works... which expands to
- ```clojure
  (def hi (with-meta (fn [] (do "hi")) {:name "hi"})
  ```
- And that works! What is going on??? It works in my older exercism-express app, but not in the main branch of my current app, so it's not something I broke just now. This is weird.
- What is it about with-meta that makes it work?
- The error is being thrown by the printer.
- Ah... I got it. I forgot that it uses the name metadata to print the function. ok, mystery solved.
- I can't figure out why the log in my function doesn't fire when the lambda is called.
- If I *define* a function with defn, it does. But why not as a lambda? Isn't it the same thing?
- And it gets weirder. The log fires, assuming the function is called, on `(defn hi [] "hi")`, or during its macroexpansion, but not if we just call `(def hi (with-meta (fn [] (do "hi")) {:name "hi"}))`, which should be the same thing. Genuinely confusing. Maybe... no I don't have any ideas. This isn't actually critical to the problem at hand but now I'm focused on it. It doesn't make sense.
- I've got it like half done I think. I did the some of the plumbing without the logic for finding the correct arity. ATM it just uses the first one:
- ```clojure
  (defn a
    ([] (fn
          ([] "no args")
          ([n] (str "one arg: " n))))
    ([n] (fn
           ([] "no args")
           ([n] (str "one arg: " n)))))
  
  ((a))
  ((a) "hi") => "no args" 
  ```
- I'm checking the arity at the call site, but I still need to pass it to wherever the logic will take place.
- This is the current `multifn`:
- ```js
  export function multifn(Eval, Env, bodies, env) {
      console.log("[multifn] bodies:", PRINT(bodies))
      var fn = function () {
          return bodies
          //return Eval(ast, new Env(env, params, arguments));
      }
      fn.__meta__ = null;
      fn.__ast__ = bodies[0][1];
      fn.__gen_env__ = function (args) { return new Env(env, bodies[0][0], args)}
      fn._ismacro_ = false;
      console.log("[multifn] fn has ", bodies.length, " arity bodies")
      return fn;
  }
  ```
- The `bodies[0][1];` part is wrong. But if I just keep the whole list of bodies set to `__ast__`, then the interpreter can probably pick the right one. Seems like an elegant plan!
- One more thing, this part:
- ```js
  fn.__gen_env__ = function (args) {
          return new Env(env, bodies[0][0], args)
      }
  ```
- At the time that is called... well, all we need to do is dispatch on args.length:
- ```js
  export function multifn(Eval, Env, bodies, env) {
      console.log("[multifn] bodies:", PRINT(bodies))
      var fn = function () {
          return bodies
          //return Eval(ast, new Env(env, params, arguments));
      }
      fn.__meta__ = null;
      fn.__ast__ = bodies;
      fn.__gen_env__ = function (args) {
          return new Env(env, bodies[args.length][0], args)
      }
      fn._ismacro_ = false;
      console.log("[multifn] fn has ", bodies.length, " arity bodies")
      return fn;
  }
  ```
- It's nearly done, but not quite. The commented line is very important, and the previous example probably only worked because the ast was just a string.
- I think this should do it:
- ```js
  export function multifn(Eval, Env, bodies, env) {
      console.log("[multifn] bodies:", PRINT(bodies))
      var fn = function () {
          return Eval(bodies[arguments.length][1], 
              new Env(env, bodies[arguments.length][0], arguments));
      }
      fn.__meta__ = null;
      fn.__ast__ = bodies;
      fn.__gen_env__ = function (args) {
          return new Env(env, bodies[args.length][0], args)
      }
      fn._ismacro_ = false;
      console.log("[multifn] fn has ", bodies.length, " arity bodies")
      return fn;
  }
  ```
- Ah, crap. The unary arity doesn't work on the outer fn:
- ```clojure
  (defn a
    ([]
    (fn
      ([] "no args")
      ([n] (str "one arg: " n))))
    ([n]
     (fn
      ([] "no args")
      ([n] (str "one arg: " n)))))
  
  ((a 1)) => 
  Error: 'n' not found 
  ```
- But both arities of the nullary arity of the outer fn work, which are the ones I tested:
- ```clojure
  ((a)) => "no args" 
  ((a) "hi") => "one arg: hi"
  ```
- Darn... guess I'm not done yet. Well the important thing is it didn't break anything else. Still 102 tests passing.
- ## Debug
	- Alright so let's crack this open... first, I made the test case a bit more comprehensive:
	- ```clojure
	  (defn a
	    ([] (fn ([] "outer: 0 args, inner: 0 args")
	            ([n] (str "outer: 0 args, inner: 1 arg: " n))))
	    ([n] (fn ([] "outer: 1 arg, inner: 0 args")
	             ([n] (str "outer: 1 arg, inner: 1 arg: " n)))))
	  
	  ((a))
	  ((a) "hi")
	  
	  ((a 1))
	  ((a 1) "hi")
	  ```
	- The problem arises before we even get to the inner function (or would that be considered the outer fn? I'm confused now...)
	- ```clojure
	  
	  ```
	- The good news is, this is forcing me to finally understand the way functions are defined and called.
	- But check it out... it works if we get rid of the defn:
	- ```clojure
	  (((fn
	    ([] (fn ([] "outer: 0 args, inner: 0 args")
	            ([n] (str "outer: 0 args, inner: 1 arg: " n))))
	    ([n] (fn ([] "outer: 1 arg, inner: 0 args")
	             ([n] (str "outer: 1 arg, inner: 1 arg: " n)))))))
	  => "outer: 0 args, inner: 0 args"
	  
	  (((fn
	    ([] (fn ([] "outer: 0 args, inner: 0 args")
	            ([n] (str "outer: 0 args, inner: 1 arg: " n))))
	    ([n] (fn ([] "outer: 1 arg, inner: 0 args")
	             ([n] (str "outer: 1 arg, inner: 1 arg: " n)))))
	    1)) => "outer: 1 arg, inner: 0 args" 
	  
	  (((fn
	    ([] (fn ([] "outer: 0 args, inner: 0 args")
	            ([n] (str "outer: 0 args, inner: 1 arg: " n))))
	    ([n] (fn ([] "outer: 1 arg, inner: 0 args")
	             ([n] (str "outer: 1 arg, inner: 1 arg: " n)))))
	   1) "hi") => "outer: 1 arg, inner: 1 arg: hi" 
	  
	  (((fn
	    ([] (fn ([] "outer: 0 args, inner: 0 args")
	            ([n] (str "outer: 0 args, inner: 1 arg: " n))))
	    ([n] (fn ([] "outer: 1 arg, inner: 0 args")
	             ([n] (str "outer: 1 arg, inner: 1 arg: " n))))))
	   "hi") => "outer: 0 args, inner: 1 arg: hi" 
	  ```
- Ha! It's not a bug! It's just unimplemented. That's what happens when you code with ADHD and brain damage.
- Here's the function that `defn` needs to handle:
- ```clojure
  (defn a
    ([] (fn ([] "outer: 0 args, inner: 0 args")
            ([n] (str "outer: 0 args, inner: 1 arg: " n))))
    ([n] (fn ([] "outer: 1 arg, inner: 0 args")
             ([n] (str "outer: 1 arg, inner: 1 arg: " n)))))
  ```
- Normal fn:
- ```clojure
  (defn a [] "outer: 0 args, inner: 0 args")
  ```
- Return value of `defn` for docstring:
- ```clojure
  
  ```
- Without docstring:
- ```clojure
  `(def ~name (with-meta (fn ~(first fdecl) (do ~@(rest fdecl)))
                      ~{:name (str name)}))
  ```
- I've got the function set up for arity handling, but need to implement the return values for arities for both of those cases. The no-docstring one is slightly simpler so perhaps I'll start with that.
- Here's just the function sig output:
- ```clojure
  (fn ~(first fdecl) (do ~@(rest fdecl)))
  ```
- Actually, I think it's even simpler than the usual case, because we don't need to worry about arglists at all.
- Yes! Here it is:
- ```clojure
  (defmacro defn [name & fdecl]
    (if (string? (first fdecl))
      (if (list? (second fdecl))
        `(def ~name (with-meta (fn ~@(rest fdecl))
                      ~{:name (str name) :doc (first fdecl)}))
        `(def ~name (with-meta (fn ~(second fdecl) (do ~@(nnext fdecl)))
                      ~{:name (str name) :doc (first fdecl)})))
      (if (list? (first fdecl))
        `(def ~name (with-meta (fn ~@fdecl)
                      ~{:name (str name)}))
        `(def ~name (with-meta (fn ~(first fdecl) (do ~@(rest fdecl)))
                      ~{:name (str name)})))))
  ```
- ## Variadic overloads
	- And also... need to correct a big error I just glossed over. It assumes the arity bodies are sorted by number of args! oops
	- I'm looking at the Clojure compiler now, the `FnExpr` part. One of the first things it does is look for a `RestFn` (variadic overload) and store it if it exists.
	- Let's craft a few examples. For extra fun, I'll declare the bodies backwards from the usual order:
	- ```clojure
	  (defn a
	    ([a & bs] (str "variadic: " a " and " bs))
	    ([a b] (str "binary: " a " and " b))
	    ([n] (str "unary: " n))
	    ([] "nullary"))
	  ```
	- So, I think everything is fine on the function definition side. It's recognized as a multifn and the bodies are stored. It's the calling part that needs changing, both in the multifn type and the interpreter call step.
	- Struggling to work out the logic for this.
	- Do we need to handle arity selection differently if there is a rest param in one of the bodies?
	- I'm thinking there could be something like
	- ```clojure
	  (defn a
	    ([a b] "binary: " a b)
	    ([a b c] (str "trinary: " a b c))
	    ([a & bs] (str "variadic: " a bs)))
	  
	  (a 1 2)
	  (a 1 2 3)
	  (a 1 2 3 4)
	  ```
	- It's the `(a 1 2 3)` that I think could trip it up.
	- Yeah, it seems to be the case. If there is a variadic, it can't just use it. It needs to first check if there's a matching fixed arity. Meaning, it needs to *exclude* the variadic one.
	- We can modify the `findArity` function to be `findFixedArity`:
	- ```js
	  function findFixedArity(arity, bodies) {
	      for (let i = 0; i < bodies.length; i++) {
	          if (bodies[i][0].length === arity && !bodies[i][0].toString().includes('&')) {
	              return bodies[i][0]
	          }
	      }
	  }
	  ```
	- And... it works! But there's still something wrong with the env or something.
	- I uh, finished this. It totally works!
	- I wonder if `juxt` will work now?
	- It already does, actually. Except for keyword functions, though. Currently those only work when evaluating lists, they aren't proper functions. I don't even know how I'd do that...