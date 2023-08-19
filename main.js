import './style.css'
import { EditorView, basicSetup } from 'codemirror'
import { EditorState } from '@codemirror/state'
import { clojure } from "./src/clojure"
import solutions from './test/exercises.json';
import testSuites from './test/tests.json';
import { evalString, deftests, clearTests } from "./src/interpreter"

let editorState = EditorState.create({
  doc: `(defn hi
    "my docstring"
    []
    "hewwo")`,
  extensions: [basicSetup, clojure()]
})

let view = new EditorView({
  state: editorState,
  parent: document.querySelector('#app')
})

let testState = EditorState.create({
  readOnly: true,
  extensions: [
    //EditorView.editable.of(false),
    basicSetup, clojure()]
})

let testView = new EditorView({
  state: testState,
  parent: document.querySelector('#test')
})

let topLevelText = "Alt+Enter = Eval top-level form"
let keyBindings = "<strong>Key bindings:</strong>,Shift+Enter = Eval cell," +
  topLevelText + ",Ctrl/Cmd+Enter = Eval at cursor";
keyBindings = keyBindings.split(',');
for (let i = 0; i < keyBindings.length; i++)
  keyBindings[i] = "" + keyBindings[i] + "<br>";
keyBindings = keyBindings.join('');
document.getElementById("keymap").innerHTML = keyBindings;

let exercise
const results = document.getElementById("results")

function loadExercise(slug) {
  results.innerHTML = ""
  exercise = slug
  const k = slug.replaceAll("-", "_")
  const src = solutions[k].trim()
  //console.log("loading test suite", testSuites[k + "_test"])
  const testSuite = testSuites[k + "_test"].trim()
  const doc = view.state.doc.toString()
  const testDoc = testView.state.doc.toString()
  const end = doc.length
  view.dispatch({
    changes: { from: 0, to: end, insert: src },
    selection: { anchor: 0, head: 0 }
  })
  testView.dispatch({
    changes: { from: 0, to: testDoc.length, insert: testSuite },
    selection: { anchor: 0, head: 0 }
  })
}

function testSolution(slug) {
  loadExercise(slug)
  const k = slug.replaceAll("-", "_")
  const src = solutions[k].trim()
  let doc = view.state.doc.toString()
  const end = doc.length
  view.dispatch({
    changes: { from: 0, to: end, insert: src },
    selection: { anchor: 0, head: 0 }
  })
  doc = view.state.doc.toString()
  clearTests()
  const testSuite = testSuites[k + "_test"].trim()
  try {
    evalString("(do " + doc + ")")
  } catch (error) {
    results.innerHTML = error
    results.style.color = 'red';
    return null
  }
  try {
    evalString("(do " + testSuite + ")")
  } catch (error) {
    results.innerHTML = error
    results.style.color = 'red';
    return null
  }
  let fails = []
  for (const test of deftests) {
    if (test.result.includes(false)) {
      fails.push(test.test.value)
    }
  }
  //console.log("deftests:", deftests)
  //console.log("fails:", fails)
  const uniqueFails = [...new Set(fails)];
  if (uniqueFails.length == 1) {
    results.innerHTML = "1 fail: " + uniqueFails[0]
    results.style.color = 'red';
  } else if (uniqueFails.length > 1) {
    results.innerHTML = uniqueFails.length + " fails: " + uniqueFails.join(", ")
    results.style.color = 'red';
  }
  else {
    results.innerHTML = "Passed 😍"
    results.style.color = 'green';
  }
}

function shuffle(array) {
  let currentIndex = array.length, randomIndex;
  while (currentIndex != 0) {
    randomIndex = Math.floor(Math.random() * currentIndex);
    currentIndex--;
    [array[currentIndex], array[randomIndex]] = [
      array[randomIndex], array[currentIndex]];
  }

  return array;
}

const exercisesToTest = ['isogram', 'powerset', 'go_counting', 'largest_series_product', 'reversi', 'sublist', 'camel', 'robot_simulator', 'inject', 'rna_transcription', 'seq_prons', 'pov', 'compress', 'spaz_out', 'word_chain', 'cartesian', 'wordy', 'luhn', 'my_trampoline', 'say', 'sieve', 'isbn_verifier', 'symmetric', 'happy', 'scrabble_score', 'allergies', 'protein_translation', 'mycomp', 'flipper', 'secret_handshake', 'hexadecimal', 'minesweeper', 'graph', 'lt', 'my_merge_with', 'poker', 'kindergarten_garden', 'k', 'black_box', 'yacht', 'word_count', 'ttt2', 'binary', 'nth_prime', 'perfect_numbers', 'eulerian', 'gigasecond', 'rn', 'matching_brackets', 'leap', 'pangram', 'queen_attack', 'tri_path', 'all_your_base', 'key_val', 'space_age', 'my_group_by', 'binary_search', 'binary_search_tree', 'flatten_array', 'getcaps', 'f', 'change', 'etl', 'pascal', 'pig_latin', 'word_sort', 'perfect_square', 'strain', 'intervals', 'grains', 'phone_number', 'sh', 'prime_factors', 'collatz_conjecture', 'nucleotide_count', 'makeflat', 'run_length_encoding', 'clock', 'find_path', 'longest_subseq', 'bob', 'veitch', 'lev', 'grade_school', 'uce', 'spiral_matrix', 'cards', 'meetup', 'crypto_square', 'ss', 'anagram', 'dominoes', 'ttt', 'lazy', 'atbash_cipher', 'ps', 'conway', 'cw', 'diamond']
//const exercisesToTest = shuffle(Object.keys(solutions))
function randExercise() {
  return exercisesToTest[Math.floor(Math.random() * exercisesToTest.length)]
}

function testExercises() {
  let passes = []
  let fails = []
  for (let exercise = 0; exercise < exercisesToTest.length; exercise++) {
    console.log("Testing ", exercisesToTest[exercise])
    testSolution(exercisesToTest[exercise])
    if (results.innerHTML === "Passed 😍") {
      console.log("exercise passed")
      passes.push(exercisesToTest[exercise])
      results.innerHTML = passes.length + " tests passed 😍"
    } else {
      results.innerHTML = passes.length + " tests passed, " + exercisesToTest[exercise] + " failed"
      fails.push(exercisesToTest[exercise])
    }
  }
  console.log("Passes:", passes)
  console.log("Fails:", fails)
}

function testExercisesUntilFail() {
  let passes = []
  var fails = []
  while (fails.length === 0) {
    for (let exercise = 0; exercise < exercisesToTest.length; exercise++) {
      console.log("Testing ", exercisesToTest[exercise])
      testSolution(exercisesToTest[exercise])
      if (results.innerHTML === "Passed 😍") {
        console.log("exercise passed")
        passes.push(exercisesToTest[exercise])
        results.innerHTML = passes.length + " tests passed 😍"
      } else {
        results.innerHTML = passes.length + " tests passed, " + exercisesToTest[exercise] + " failed"
        fails.push(exercisesToTest[exercise])
      }
    }
    console.log("Passes:", passes)
    console.log("Fails:", fails)
  }
}

//testSolution(randExercise())
//testSolution("proverb")
//loadExercise("lev")
//testExercises()
//testExercisesUntilFail()