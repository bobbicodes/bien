# bien

An interactive programming environment based on Lisp

## Status

Bien is based on [MAL (Make-a-Lisp)](https://github.com/kanaka/mal), which is a pedagogical Lisp interpreter inspired by Clojure. In a nutshell, this means that it has a set of built-in data structures besides lists, and is designed for functional programming using immutability by default with an `atom` type for controlled mutation. A goal is to support as much of Clojure as possible so it can be used for teaching Clojure.

I'd estimate that the goal is around 50% complete *by quantity*, though the unimplemented half is likely much more involved than the first.

### Differences from Clojure

1. No lazy sequences. There is however very limited support for certain types of infinite sequences, for example, these work:

```clojure
(take 5 (range)) => (0 1 2 3 4)
(take 5 (iterate inc 5)) => (5 6 7 8 9)
(take 5 (cycle [1 2 3])) => (1 2 3 1 2)
```

2. No protocols. Chris Houser (Chouser) has created a [sketch](https://gist.github.com/Chouser/6081ea66d144d13e56fc) of this feature which is included in the [MAL project](https://github.com/kanaka/mal/blob/master/impls/lib/protocols.mal), but I have not added them yet. The honest truth is, I never use them in my own code. Perhaps I should? Clojurescript is built using heavy use of protocols, and it would be a smart thing for me make better use of that codebase. We'll see, time will tell.

3. Limited destructuring. Specifically, I've implemented sequential destructuring and hooked it up to `let`, but it hasn't been thoroughly tested yet so it might even cause problems. If this is the case, simply use the `let*` which is the special form built into the interpreter which does not use destructuring. Associative destructuring is still not working.

I plan to update this section regularly with detailed descriptions of various features as I make progress.


## Running

In the project root, run:

```
npm install
npm run dev
```

## Testing

The application has a built-in testing framework based on 185 coding exercises taken from 4clojure and Exercism. To run it, uncomment the last line of `main.js`:

```javascript
testExercises()
```
