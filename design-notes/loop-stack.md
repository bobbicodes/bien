- I wish the Clojure compiler was more comprehensible to me.
- It appears that there are 2 dynamic vars for loop locals and loop label:
- ```java
  //vector<localbinding>
  static final public Var LOOP_LOCALS = Var.create().setDynamic();
  
  //Label
  static final public Var LOOP_LABEL = Var.create().setDynamic();
  ```
- wtf is a label?
- I actually got an idea from this one bit of code:
- ```java
  Var.pushThreadBindings(
  					RT.mapUniqueKeys(
  							METHOD, method,
  							LOCAL_ENV, LOCAL_ENV.deref(),
  							LOOP_LOCALS, null,
  							NEXT_LOCAL_NUM, 0
                              ,CLEAR_PATH, pnode
                              ,CLEAR_ROOT, pnode
                              ,CLEAR_SITES, PersistentHashMap.EMPTY
                              ,METHOD_RETURN_CONTEXT, RT.T
                          ));
  ```
- It was the `NEXT_LOCAL_NUM, 0` bit. I could just use numbers to keep track of the bodies/bindings/envs or whatever.
- Where's my old friend, that mutually recurring loop system?
- ```clojure
  (defn loop1 [coll]
    (loop [s coll res []]
      (if (empty? s) res
        (recur (rest s) (conj res (first s))))))
  
  (defn loop2 [colls]
    (loop [s colls res []]
      (if (empty? s) res
        (recur (rest s) (conj res (loop1 (first s)))))))
  
  (loop2 [[1 2] [3 4]])
  ```
- So, instead of using a single var for the loop data (bindings, body, env), I use arrays for each:
- ```js
  var loopBodies = []
  var loopBindings = []
  var loopEnvs = []
  ```
- And the interpreter `loop` clause will be like this:
- ```js
  case "loop":
       var loopEnv = new Env(env)
       var loopBody = [types._symbol('do')].concat(ast.slice(2))
       for (var i = 0; i < a1.length; i += 2) {
            loopEnv.set(a1[i], EVAL(a1[i + 1], loopEnv))
       }
       loopBindings.push(a1)
       loopEnvs.push(loopEnv)
       loopBodies.push(loopBody)
       ast = a2;
       env = loopEnv;
       break
  ```
- This is the current `recur` clause:
- ```js
  // check if the loop body has a let expr
  // if so, copy its locals into the loop_env
  if (hasLet(loopAST)) {
      for (const key in let_env.data) {
          if (Object.hasOwnProperty.call(let_env.data, key)) {
              loop_env.set(types._symbol(key), let_env.data[key])
          }
      }
  }
  const recurAST = eval_ast(ast.slice(1), loop_env)
  for (var i = 0; i < loopVars.length; i += 1) {
      loop_env.set(loopVars[i], recurAST[i]);
  }
  ast = loopAST[0]
  break;
  ```
- I'm going to delete it for now, which is why I'm sticking it here because I'll probably need that bit again.
- # Fuck
	- God, no matter what I do it doesn't work. I put some printlns in the functions to make it clear what is happening:
	- ```clojure
	  (defn loop1 [coll]
	    (loop [s coll res1 []]
	      (do (println "loop1 coll:" coll " res:" res1)
	      (if (empty? s) res1
	        (recur (rest s) (conj res1 (first s)))))))
	  
	  (defn loop2 [colls]
	    (loop [s colls res2 []]
	      (do (println "loop2 colls:" colls " res:" res2)
	      (if (empty? s) res2
	        (recur (rest s) (conj res2 (loop1 (first s))))))))
	  
	  (loop2 [[1 2] [3 4]])
	  
	  loop2 colls: [[1 2] [3 4]]  res: []
	  loop1 coll: [1 2]  res: []
	  loop1 coll: [1 2]  res: [1]
	  loop1 coll: [1 2]  res: [1 2]
	  loop2 colls: [[1 2] [3 4]]  res: [[1 2]]
	  loop1 coll: [3 4]  res: []
	  loop1 coll: [3 4]  res: [3]
	  loop1 coll: [3 4]  res: [3 4]
	  loop2 colls: [[1 2] [3 4]]  res: [[1 2] [3 4]]
	  [[1 2] [3 4]]
	  ```
	- So we can kind of see why it's difficult, it calls the outer loop 3 times and the inner loop 6 times.
	- Maybe if I have recur return to the loop itself, instead of just the loop body?
	- I asked the question on Slack, and emccue said to just use a `while` loop:
	- ```js
	  Object s = ...;
	  Object res1 = ...;
	  
	  loop:
	  while (true) {
	    if (...) {
	       s = rest(s);
	       res1 = conj(res1, first(s));
	    }
	    else {
	       break loop;
	    }
	  }
	  ```
	- But if I'm thinking right... if I jump back to the actual `(loop ...)`... it might work
	- ```js
	  [s [1 2] res []]
	  ```
	- how do I replace every other element in the binding vector with