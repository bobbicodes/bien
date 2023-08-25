# lisp-tutorial

In this tutorial we will be building a programming language and learning environment based on [MAL](https://github.com/kanaka/mal) (Make-a-Lisp). It is a teaching language inspired by Clojure that has been implemented in over 90 languages. We will be building it in JavaScript, mainly because it easily runs in the browser, and we will be taking advantage of that by using a custom Codemirror editor to evaluate forms instead of the usual REPL prompt. It will be slightly more complicated to begin with, but will pay for itself by facilitating a more convenient and full-featured workflow.

To follow along, generate your own instance of the programming environment by forking the [web-editor-template](https://github.com/bobbicodes/web-editor-template).

## Running

In the project root, run:

```
npm install
npm run dev
```

## Testing

The application has a built-in testing framework based on over 150 coding exercises taken from 4clojure and Exercism. To run it, uncomment the last line of `main.js`:

```javascript
testExercises()
```
