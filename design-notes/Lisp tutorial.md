- In this tutorial we will be making a Lisp from scratch. But since it is much more convenient to work in my online editor, it will start from a template repository which I will make now.
- It's done! So now I'll fork that as a new repo, lisp-tutorial.
- Now, if we try to evaluate something, we get `Error: evalString is not defined`. Let's make an `evalString` function that will just pass through its input.
- So now we need an environment.
- I created 2 branches. `step-0` and `step-1-env`.
- This is the env:
- ```js
  export function Env(outer, binds, exprs) {
      this.data = {};
      this.outer = outer || null;
  
      if (binds && exprs) {
          for (var i=0; i<binds.length;i++) {
              if (binds[i].value === "&") {
                  this.data[binds[i+1].value] = Array.prototype.slice.call(exprs, i);
                  break;
              } else {
                  this.data[binds[i].value] = exprs[i];
              }
          }
      }
      return this;
  }
  Env.prototype.find = function (key) {
      if (key.value in this.data) { return this; }
      else if (this.outer) {  return this.outer.find(key); }
      else { return null; }
  };
  Env.prototype.set = function(key, value) {
      this.data[key.value] = value;
      return value;
  };
  Env.prototype.get = function(key) {
      var env = this.find(key);
      if (!env) { 
          throw new Error("'" + key.value + "' not found"); }
      return env.data[key.value];
  };
  ```
- I tried to write it differently, because I hate constructors. But it was giving me problems at one point. I might try again sometime, but for now I could just take it as something that works.
- I think I should begin without the env machinery and just use a normal object, like the built-in MAL steps do it. It lets us demonstrate the reading part with less complication.
- This is a nice article that explains how to accomplish things without `this` or `new`: https://www.toptal.com/javascript/es6-class-chaos-keeps-js-developer-up
- I'm immediately convinced, actually relieved. Let's see if I can summarize it
- I just noticed that there's an ES6 version of the MAL interpreter! How did I miss that? And check out the Env:
- ```js
  export function new_env(outer={}, binds=[], exprs=[]) {
      var e = Object.setPrototypeOf({}, outer)
      // Bind symbols in binds to values in exprs
      for (var i=0; i<binds.length; i++) {
          if (Symbol.keyFor(binds[i]) === "&") {
              e[binds[i+1]] = exprs.slice(i) // variable length arguments
              break
          }
          e[binds[i]] = exprs[i]
      }
      return e
  }
  export const env_get = (env, sym) => {
      if (sym in env) { return env[sym] }
      throw Error(`'${Symbol.keyFor(sym)}' not found`)
  }
  export const env_set = (env, sym, val) => env[sym] = val
  ```
- Yes, that's the entire file!
- I'm going to start over completely, and really try to do it MY WAY. Let's go.
- # Ground zero
	- I'll use the template I made. Wait... no. I'll use the same lisp-tutorial repo and just modify the branches as I see fit, and start putting in actual markdown.
	- I'll just write the chapters here because it's familiar. Just like the Clojure Interpreter step X pages, but will extend these Lisp tutorial pages.
- # Step 0
- Let's make a [[Lisp tutorial - intro]]
- ok that's good for now. I put it in the main project readme. Now let's go into the reader and printer.
- # Step 1 - read/print
	- ## Reader
		- We will create the `reader.js` file which will contain our stateful Reader constructor:
		- ```js
		  function Reader(tokens) {
		      this.tokens = tokens.map(function (a) { return a; });
		      this.position = 0;
		  }
		  Reader.prototype.next = function () { return this.tokens[this.position++]; }
		  Reader.prototype.peek = function () { return this.tokens[this.position]; }
		  ```
		- The Reader takes an array of tokens and keeps track of a position as it is read. It has 2 methods, `next` and `peek`, both of which return the token at the current position. `next` increments the position while `peek` does not. `peek` will be used to look ahead so that we can dispatch on whatever the first token is.
	- ## Tokenizer
		- ```js
		  function tokenize(str) {
		      const re = /[\s,]*(~@|[\[\]{}()'`~^@]|"(?:\\.|[^\\"])*"?|;.*|[^\s\[\]{}('"`,;)]*)/g
		      let match = null
		      let results = []
		      while ((match = re.exec(str)[1]) != '') {
		          if (match[0] === ';') { continue }
		          results.push(match)
		      }
		      return results
		  }
		  ```
		-
	- ## read-str
		- This is the main function exported from the reader module:
		- ```js
		  export function read_str(str) {
		      var tokens = tokenize(str)
		      if (tokens.length === 0) { throw new BlankException() }
		      return read_form(new Reader(tokens))
		  }
		  ```
		- It takes a string of code from our editor, calls the tokenizer and calls `read_form`, which initializes a Reader with the array of tokens.
	- ## `read_form`
	- ```js
	  function read_form(reader) {
	      var token = reader.peek()
	      switch (token) {
	      // reader macros/transforms
	      case ';': return null // Ignore comments
	      case '\'': reader.next()
	                 return [Symbol.for('quote'), read_form(reader)]
	      case '`': reader.next()
	                return [Symbol.for('quasiquote'), read_form(reader)]
	      case '~': reader.next()
	                return [Symbol.for('unquote'), read_form(reader)]
	      case '~@': reader.next()
	                 return [Symbol.for('splice-unquote'), read_form(reader)]
	      case '^': reader.next()
	                var meta = read_form(reader)
	                return [Symbol.for('with-meta'), read_form(reader), meta]
	      case '@': reader.next()
	                return [Symbol.for('deref'), read_form(reader)]
	  
	      // list
	      case ')': throw new Error("unexpected ')'")
	      case '(': return read_list(reader)
	  
	      // vector
	      case ']': throw new Error("unexpected ']'")
	      case '[': return read_vector(reader)
	  
	      // hash-map
	      case '}': throw new Error("unexpected '}'")
	      case '{': return read_hash_map(reader)
	  
	      // atom
	      default:  return read_atom(reader)
	      }
	  }
	  ```
	- This shows how the Reader methods are used to parse the different kinds of forms. We `peek` at the first one and switch on it.
	- We then have special functions to handle the basic data structures, lists, vectors, hash-maps, and atoms which are individual values.
	- ## `read_list`
		- ```js
		  function read_list(reader, start, end) {
		      start = start || '('
		      end = end || ')'
		      var ast = []
		      var token = reader.next()
		      if (token !== start) {
		          throw new Error("expected '" + start + "'")
		      }
		      while ((token = reader.peek()) !== end) {
		          if (!token) {
		              throw new Error("expected '" + end + "', got EOF")
		          }
		          ast.push(read_form(reader))
		      }
		      reader.next()
		      return ast
		  }
		  ```
	- ## `read_vector`
		- ```js
		  function read_vector(reader) {
		      return Vector.from(read_list(reader, '[', ']'));
		  }
		  ```
	- ## `read_hash_map`
		- ```js
		  function read_hash_map(reader) {
		      return _assoc_BANG(new Map(), ...read_list(reader, '{', '}'))
		  }
		  ```
	- ## `read_atom`
		- ```js
		  function read_atom (reader) {
		      const token = reader.next()
		      //console.log("read_atom:", token)
		      if (token.match(/^-?[0-9]+$/)) {
		          return parseInt(token,10)        // integer
		      } else if (token.match(/^-?[0-9][0-9.]*$/)) {
		          return parseFloat(token,10)     // float
		      } else if (token.match(/^"(?:\\.|[^\\"])*"$/)) {
		          return token.slice(1,token.length-1)
		              .replace(/\\(.)/g, (_, c) => c === "n" ? "\n" : c)
		      } else if (token[0] === "\"") {
		          throw new Error("expected '\"', got EOF");
		      } else if (token[0] === ":") {
		          return _keyword(token.slice(1))
		      } else if (token === "nil") {
		          return null
		      } else if (token === "true") {
		          return true
		      } else if (token === "false") {
		          return false
		      } else {
		          return Symbol.for(token) // symbol
		      }
		  }
		  ```
- # types
	- Let's create the `types.js` module.  For now we'll include only what we need for the reader, namely `_keyword`, `_assoc_BANG`, and `Vector`.
	- Put the import statement at the top of `reader.js`:
	- ```js
	  import { _keyword, _assoc_BANG, Vector } from './types';
	  ```
	- ```js
	  // Keywords
	  export const _keyword = obj => _keyword_Q(obj) ? obj : '\u029e' + obj
	  export const _keyword_Q = obj => typeof obj === 'string' && obj[0] === '\u029e'
	  
	  // Vectors
	  export class Vector extends Array { }
	  
	  // Maps
	  export function _assoc_BANG(hm, ...args) {
	      if (args.length % 2 === 1) {
	          throw new Error('Odd number of assoc arguments')
	      }
	      for (let i=0; i<args.length; i+=2) { hm.set(args[i], args[i+1]) }
	      return hm
	  }
	  ```
	- Import `read_str` from `reader.js`:
	- ```js
	  import { read_str } from './reader'
	  ```
	- Modify the `READ` function to use it:
	- ```js
	  const READ = str => read_str(str)
	  ```
- At this point we can read the basic forms:
- ```clojure
  (1 2 3) => 1,2,3
  [1 2 3] => 1,2,3
  {:a 2} => [object Map] 
  1 => 1
  "hi" => hi 
  1.5 => 1.5 
  :a => ʞa
  ```
-