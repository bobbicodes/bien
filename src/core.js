import { read_str } from './reader.js';
import { _pr_str, _println } from './printer.js'
import * as types from './types.js'
import { repl_env, walk, evalString } from './interpreter.js';
import zip from './clj/zip.clj?raw'

// Errors/Exceptions
function mal_throw(exc) { throw new Error(exc); }

function thrown_Q(x) {
    try {
        var exp = x
    } catch (error) {
        return true
    }
}

// String functions
function pr_str() {
    return Array.prototype.map.call(arguments, function (exp) {
        return _pr_str(exp, true);
    }).join(" ");
}

function str() {
    const args = Array.from(arguments).filter(v => v !== null);
    return Array.prototype.map.call(args, function (exp) {
        return _pr_str(exp, false);
    }).join("");
}

function prn() {
    _println.apply({}, Array.prototype.map.call(arguments, function (exp) {
        return _pr_str(exp, true);
    }));
}

function println() {
    _println.apply({}, Array.prototype.map.call(arguments, function (exp) {
        return _pr_str(exp, false);
    }));
}

function slurp(f) {
    if (typeof require !== 'undefined') {
        return require('fs').readFileSync(f, 'utf-8');
    } else {
        var req = new XMLHttpRequest();
        req.open("GET", f, false);
        req.send();
        if (req.status == 200) {
            return req.responseText;
        } else {
            throw new Error("Failed to slurp file: " + f);
        }
    }
}


// Number functions
function time_ms() { return new Date().getTime(); }


// Hash Map functions
function assoc(src_hm) {
    if (src_hm === null) {
        src_hm = new Map()
    }
    var hm = types._clone(src_hm);
    var args = [hm].concat(Array.prototype.slice.call(arguments, 1));
    return types._assoc_BANG.apply(null, args);
}

function dissoc(src_hm) {
    var hm = types._clone(src_hm);
    var args = [hm].concat(Array.prototype.slice.call(arguments, 1));
    return types._dissoc_BANG.apply(null, args);
}

function get(coll, key, notfound) {
    if (types._vector_Q(coll)) {
        return coll[key]
    }
    if (types._string_Q(coll)) {
        return coll[key]
    }
    if (types._hash_map_Q(coll)) {
        for (const [k, value] of coll) {
            if (types._equal_Q(k, key)) {
                return value
            }
        }
        return notfound
    }
    if (coll != null) {
        return coll.get(key) || notfound
    } else {
        return null;
    }
}

function contains_Q(coll, key) {
    if (coll === null) {
        return false
    }
    if (types._hash_map_Q(coll)) {
        for (const [k, value] of coll) {
            if (types._equal_Q(k, key)) {
                return true
            }
        }
    }
    if (types._set_Q(coll)) {
        return coll.has(key)
    }
    if (key in coll) { return true; } else { return false; }
}

function keys(hm) { return Array.from(hm.keys()) }
function vals(hm) { return Array.from(hm.values()) }


// Sequence functions
export function cons(a, b) { return [a].concat(b); }

function concat(lst) {
    lst = lst || [];
    return lst.concat.apply(lst, Array.prototype.slice.call(arguments, 1));
}
function vec(lst) {
    if (types._list_Q(lst)) {
        var v = Array.prototype.slice.call(lst, 0);
        v.__isvector__ = true;
        return v;
    } else {
        return lst;
    }
}

function re_matches(re, s) {
    let matches = re.exec(s);
    if (matches && s === matches[0]) {
        if (matches.length === 1) {
            return matches[0];
        } else {
            return matches;
        }
    }
}

function re_find(re, s) {
    let matches = re.exec(s);
    if (matches) {
        if (matches.length === 1) {
            return matches[0];
        } else {
            return matches;
        }
    }
}

function re_seq(re, s) {
    if (s === null) {
        return null
    }
    const array = [...s.matchAll(re)];
    const firsts = array.map(x => x[0])
    if (firsts.length === 0) {
        return null
    }
    return firsts
}

export function nth(lst, idx, notfound) {
    if (types._iterate_Q(lst)) {
        for (let i = 0; i < idx; i++) {
            lst.next()
        }
        return lst.realized[idx]
    }
    if (idx < lst.length) { return lst[idx]; }
    else { return notfound }
}

export function first(lst) {
    if (types._lazy_range_Q(lst)) {
        return 0
    }
    return (lst === null || lst.length === 0) ? null : seq(lst)[0];
}

export function second(lst) { return (lst === null || lst.length === 0) ? null : seq(lst)[1]; }
export function last(lst) { return (lst === null || lst.length === 0) ? null : seq(lst)[seq(lst).length - 1]; }

export function rest(lst) { return (lst == null || lst.length === 0) ? [] : seq(lst).slice(1); }

function empty_Q(lst) {
    if (types._set_Q(lst) || types._hash_map_Q(lst)) {
        if (lst.size === 0) {
            return true
        } else {
            return false
        }
    }
    if (!lst) {
        return true
    }
    return lst.length === 0;
}

export function count(s) {
    if (types._hash_map_Q(s)) { return s.size; }
    if (Array.isArray(s)) { return s.length; }
    if (types._set_Q(s)) { return s.size; }
    else if (s === null) { return 0; }
    else { return Object.keys(s).length; }
}

function conj(lst) {
    if (types._iterate_Q(lst)) {
        return "TODO: implement iterate on conj"
    }
    if (types._list_Q(lst)) {
        return Array.prototype.slice.call(arguments, 1).reverse().concat(lst);
    } else if (types._vector_Q(lst)) {
        var v = lst.concat(Array.prototype.slice.call(arguments, 1));
        v.__isvector__ = true;
        return v;
    } else if (types._set_Q(lst)) {
        return lst.add(arguments[1])
    } else if (types._hash_map_Q(lst)) {
        if (arguments.length === 2 && arguments[1].size === 0) {
            return lst
        }
        var hm = new Map(lst)
        const args = Array.prototype.slice.call(arguments, 1)
        for (var i = 0; i < args.length; i++) {
            var ktoken = args[i][0],
                vtoken = args[i][1]
            hm.set(ktoken, vtoken)
        }
        return hm
    }
}

function re_pattern(s) {
    return new RegExp(s, 'g')
}

function split(s, re) {
    return s.split(re)
}

export function seq(obj) {
    if (types._list_Q(obj)) {
        return obj.length > 0 ? obj : null;
    } else if (types._iterate_Q(obj)) {
        return obj.realized
    } else if (types._vector_Q(obj)) {
        return obj.length > 0 ? Array.prototype.slice.call(obj, 0) : null;
    } else if (types._string_Q(obj)) {
        return obj.length > 0 ? obj.split('') : null;
    } else if (types._hash_map_Q(obj)) {
        return obj.size > 0 ? [...obj.entries()] : null;
    } else if (types._set_Q(obj)) {
        return Array.from(obj)
    }
    else if (obj === null) {
        return null;
    } else {
        throw new Error("seq: called on non-sequence");
    }
}


function apply(f) {
    var args = Array.prototype.slice.call(arguments, 1);
    return f.apply(f, args.slice(0, args.length - 1).concat(args[args.length - 1]));
}


// Metadata functions
export function with_meta(obj, m) {
    var new_obj = types._clone(obj);
    new_obj.__meta__ = m;
    return new_obj;
}

function meta(obj) {
    // TODO: support symbols and atoms
    if ((!types._sequential_Q(obj)) &&
        (!(types._hash_map_Q(obj))) &&
        (!(types._function_Q(obj)))) {
        throw new Error("attempt to get metadata from: " + types._obj_type(obj));
    }
    return obj.__meta__;
}


// Atom functions
function deref(atm) { return atm.val; }
function reset_BANG(atm, val) { return atm.val = val; }
function swap_BANG(atm, f) {
    var args = [atm.val].concat(Array.prototype.slice.call(arguments, 2));
    atm.val = f.apply(f, args);
    return atm.val;
}

function js_eval(str) {
    return js_to_mal(eval(str.toString()));
}

function js_method_call(object_method_str) {
    var args = Array.prototype.slice.call(arguments, 1),
        r = resolve_js(object_method_str),
        obj = r[0], f = r[1];
    var res = f.apply(obj, args);
    return js_to_mal(res);
}

function toSet() {
    return new Set(arguments[0])
}

function _union(setA, setB) {
    const _union = new Set(setA);
    for (const elem of setB) {
        _union.add(elem);
    }
    return _union;
}

function _intersection(setA, setB) {
    const _intersection = new Set();
    for (const elem of setB) {
        if (setA.has(elem)) {
            _intersection.add(elem);
        }
    }
    return _intersection;
}

function symmetricDifference(setA, setB) {
    const _difference = new Set(setA);
    for (const elem of setB) {
        if (_difference.has(elem)) {
            _difference.delete(elem);
        } else {
            _difference.add(elem);
        }
    }
    return _difference;
}

function _difference(setA, setB) {
    const _difference = new Set(setA);
    for (const elem of setB) {
        _difference.delete(elem);
    }
    return _difference;
}

function _disj(set) {
    var args = Array.from(arguments).slice(1)
    var new_set = types._clone(set)
    for (let i = 0; i < args.length; i++) {
        new_set.delete(args[i])
    }
    return new_set
}

function _is(a) {
    if (a) {
        return true
    } else {
        return false
    }
}

function resolve_js(str) {
    if (str.match(/\./)) {
        var re = /^(.*)\.[^\.]*$/,
            match = re.exec(str);
        return [eval(match[1]), eval(str)];
    } else {
        return [GLOBAL, eval(str)];
    }
}

function js_to_mal(obj) {
    if (obj === null || obj === undefined) {
        return null;
    }
    var cache = [];
    var str = JSON.stringify(obj, function (key, value) {
        if (typeof value === 'object' && value !== null) {
            if (cache.indexOf(value) !== -1) {
                // Circular reference found, discard key
                return;
            }
            // Store value in our collection
            cache.push(value);
        }
        return value;
    });
    cache = null; // Enable garbage collection
    return JSON.parse(str);
}

function map(f, s) {
    if (types._string_Q(s)) {
        s = seq(s)
    }
    return s.map(function (el) { return f(el); });
}

function int(x) {
    if (types._ratio_Q(x)) {
        return x.floor()
    }
    if (types._number_Q(x)) {
        return Math.floor(x)
    } else if (x[0] === '\\') {
        // is a char
        return x.charCodeAt(1)
    } else {
        return x.charCodeAt(0)
    }
}

function double(x) {
    return x.valueOf()
}

function char(int) {
    return String.fromCharCode(int)
}

function filter(f, lst) {
    if (types._iterate_Q(lst)) {
        return "TODO: filter iterate object"
    }
    if (!lst || lst.length === 0) {
        return []
    }
    if (types._set_Q(f)) {
        return seq(lst).filter(function (el) { return f.has(el); });
    }
    return seq(lst).filter(function (el) { return f(el); })
}

function min() {
    return Math.min.apply(null, arguments);
}

function max() {
    return Math.max.apply(null, arguments);
}

function _pow(x, n) {
    return Math.pow(x, n)
}

function sum() {
    var res = Array.from(arguments).reduce((acc, a) => acc + a, 0);
    if (Array.from(arguments).every(function (element) { return types._ratio_Q(element) })) {
        res = types._ratio(res)
    }
    return res
}

function subtract() {
    if (arguments.length === 1) {
        return 0 - arguments[0]
    }
    var res = Array.from(arguments).slice(1).reduce((acc, a) => acc - a, arguments[0]);
    if (Array.from(arguments).every(function (element) { return types._ratio_Q(element) })) {
        res = types._ratio(res)
    }
    return res
}

function product() {
    if (arguments.length === 0) {
        return 1
    }
    var res = Array.from(arguments).reduce((acc, a) => acc * a, 1);
    if (Array.from(arguments).every(function (element) { return types._ratio_Q(element) })) {
        res = types._ratio(res)
    }
    return res
}

function divide() {
    if (arguments[0] === 0) {
        return 0
    }
    if (arguments.length === 2 && Number.isInteger(arguments[0])
        && Number.isInteger(arguments[1]) && arguments[1] != 1) {
        var quotient = types._ratio({ n: arguments[0], d: arguments[1] })
        if (quotient.d === 1) {
            return quotient.n
        } else {
            return quotient
        }
    }
    var divisor = arguments[0]
    var res = Array.from(arguments).slice(1).reduce((acc, a) => acc / a, divisor);
    if (Array.from(arguments).every(function (element) { return types._ratio_Q(element) })) {
        res = types._ratio(res)
    }
    return res
}

// https://github.com/squint-cljs/squint/blob/main/src/squint/core.js
class LazyIterable {
    constructor(gen) {
        this.name = 'LazyIterable'
        this.gen = gen;
    }
    [Symbol.iterator]() {
        return this.gen();
    }
}

export function lazy(f) {
    console.log("creating lazy-iterable of", f)
    return new LazyIterable(f);
}

export function seqable_QMARK_(x) {
    // String is iterable but doesn't allow `m in s`
    return typeof x === 'string' || x === null || x === undefined || Symbol.iterator in x;
}

export function iterable(x) {
    console.log("calling iterable on", x)
    // nil puns to empty iterable, support passing nil to first/rest/reduce, etc.
    if (x === null || x === undefined) {
        return [];
    }
    if (seqable_QMARK_(x)) {
        console.log(x, "is seqable, returning")
        return x;
    }
    return Object.entries(x);
}

export class LazySeq {
    constructor(f) {
        this.name = 'LazySeq'
        this.f = f;
    }
    *[Symbol.iterator]() {
        console.log("yielding:", this.f())
        yield* this.f();
    }
}

export function take(n, coll) {
    if (empty_Q(coll)) {
        return []
    }
    if (types._lazy_range_Q(coll)) {
        return range(0, n)
    }
    if (types._lazy_seq_Q(coll)) {
        console.log("taking from:", Array.from(coll))
        return lazy(function* () {
            let i = n - 1;
            for (const x of iterable(coll)) {
                if (i-- >= 0) {
                    console.log("yielding", x)
                    yield x;
                }
                if (i < 0) {
                    return;
                }
            }
        });
    }
    if (types._iterate_Q(coll)) {
        for (let i = 0; i < n; i++) {
            coll.next()
        }
        return coll.realized.slice(0, -1)
    }
    if (types._cycle_Q(coll)) {
        const cycles = Math.floor(n / coll.coll.length)
        const mod = n % coll.coll.length
        let res = []
        for (let i = 0; i < cycles; i++) {
            res = res.concat(coll.coll)
        }
        if (mod != 0) {
            res = res.concat(coll.coll.slice(0, mod))
        }
        return res
    }
    return seq(coll).slice(0, n)
}

function drop(n, coll) {
    return seq(coll).slice(n)
}

function repeat(n, x) {
    return Array(n).fill(x)
}



// lazy ranges
// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Guide/Iterators_and_Generators#generator_functions
function* makeRangeIterator(start = 0, end = Infinity, step = 1) {
    let iterationCount = 0;
    for (let i = start; i < end; i += step) {
        iterationCount++;
        yield i;
    }
    return iterationCount;
}

function range(start, end, step) {
    if (arguments.length === 0) {
        // uses above generator
        var iterator = makeRangeIterator()
        iterator.name = 'lazyRange'
        return iterator
    }
    if (step < 0) {
        var ans = [];
        for (let i = start; i > end; i += step) {
            ans.push(i);
        }
        return ans
    }
    if (!end) {
        if (start === 0) {
            return []
        }
        return range(0, start)
    }
    var ans = [];
    if (step) {
        for (let i = start; i < end; i += step) {
            ans.push(i);
        }
        return ans
    }
    for (let i = start; i < end; i++) {
        ans.push(i);
    }
    return ans;
}

class Iterate {
    constructor(f, x) {
        this.name = 'Iterate'
        this.f = f
        this.realized = [x];
    }
    next() {
        this.realized.push(this.f(this.realized[this.realized.length - 1]))
        return this.realized;
    }
}

function iterate(f, x) {
    return new Iterate(f, x)
}

class Cycle {
    constructor(coll) {
        this.name = 'Cycle'
        this.coll = coll
    }
}

function cycle(coll) {
    return new Cycle(coll)
}

function mod(x, y) {
    return x % y
}

function sort(x) {
    if (types._string_Q(x)) {
        return x.split('').sort().join('');
    }
    if (types._list_Q(x)) {
        return x.sort()
    }
    if (types._hash_map_Q(x)) {
        return new Map(Array.from(x).sort())
    }
    if (types._set_Q(x)) {
        return new Set(Array.from(x).sort())
    } else {
        var v = x.sort()
        v.__isvector__ = true;
        return v;
    }
}

function pop(lst) {
    if (types._list_Q(lst)) {
        return lst.slice(1);
    } else {
        var v = lst.slice(0, -1);
        v.__isvector__ = true;
        return v;
    }
}

function peek(lst) {
    if (types._list_Q(lst)) {
        return lst[0]
    } else {
        return lst[lst.length - 1]
    }
}

function upperCase(s) {
    return s.toUpperCase()
}

function isLetter(c) {
    return c.toLowerCase() != c.toUpperCase();
}

function lowerCase(s) {
    return s.toLowerCase()
}

function int_Q(x) {
    return Number.isInteger(x)
}

function _join(separator, coll) {
    if (!coll) {
        return separator.join('')
    }
    return coll.join(separator)
}

function _replace(s, match, replacement) {
    return s.replace(match, replacement)
}

function rand_int() {
    return Math.floor(Math.random() * arguments[0]);
}

function rand_nth() {
    const n = Math.floor(Math.random() * arguments[0].length)
    return arguments[0][n]
}

function _round(n) {
    return Math.round(n)
}

function _sqrt(n) {
    return Math.sqrt(n)
}

function _substring(s, start, end) {
    if (!end) {
        return s.substring(start, s.length)
    }
    return s.substring(start, end)
}

function dec2bin(dec) {
    return (dec >>> 0).toString(2);
}

function repeatedly(n, f) {
    let calls = []
    for (let i = 0; i < n; i++) {
        calls.push(f())
    }
    return calls
}

function _subvec(v, start, end) {
    return v.slice(start, end)
}

function _trim(s) {
    return s.trim()
}

function doubleEquals() {
    const nums = Array.from(arguments)
    return nums.every(v => v === nums[0])
}

// function to print env for debugging

function printEnv() {
    console.log(repl_env)
}

function require(lib) {
    switch (lib) {
        case 'zip':
            evalString("(do " + zip + ")")
            break;
        case 'set':
            evalString("(do " + clj_set + ")")
            break;
        default:
            break;
    }
}

function downloadObjectAsJson(exportObj, exportName) {
    var dataStr = "data:text/json;charset=utf-8," + encodeURIComponent(JSON.stringify(exportObj));
    var downloadAnchorNode = document.createElement('a');
    downloadAnchorNode.setAttribute("href", dataStr);
    downloadAnchorNode.setAttribute("download", exportName);
    document.body.appendChild(downloadAnchorNode); // required for firefox
    downloadAnchorNode.click();
    downloadAnchorNode.remove();
}

function spit_json(name, obj) {
    return downloadObjectAsJson(obj, name)
}

// types.ns is namespace of type functions
export var ns = {
    'env': printEnv,
    'spit-json': spit_json,
    'LazySeq': LazySeq,
    'require': require,
    'type': types._obj_type,
    '=': types.allEqual,
    '==': doubleEquals,
    'throw': mal_throw,
    'thrown?': thrown_Q,
    'nil?': types._nil_Q,
    'true?': types._true_Q,
    'is': _is,
    'false?': types._false_Q,
    'ratio?': types._ratio_Q,
    'number?': types._number_Q,
    'string?': types._string_Q,
    'symbol': types._symbol,
    'symbol?': types._symbol_Q,
    'set?': types._set_Q,
    'keyword': types._keyword,
    'keyword?': types._keyword_Q,
    'map-entry?': types._mapEntry_Q,
    're-matches': re_matches,
    're-find': re_find,
    'fn?': types._fn_Q,
    'macro?': types._macro_Q,
    'char': char,
    'int?': int_Q,
    'repeatedly': repeatedly,
    'rand-int': rand_int,
    'rand-nth': rand_nth,
    'Math/round': _round,
    'Math/sqrt': _sqrt,
    'Math/pow': _pow,
    'Integer/toBinaryString': dec2bin,
    'str/trim': _trim,
    'cycle': cycle,
    'str/split': split,
    're-pattern': re_pattern,
    'double': double,
    'pr-str': pr_str,
    'str': str,
    'prn': prn,
    'println': println,
    //'readline': readline.readline,
    'read-string': read_str,
    'slurp': slurp,
    '<': function (a, b) { return a < b; },
    '<=': function (a, b) { return a <= b; },
    '>': function (a, b) { return a > b; },
    '>=': function (a, b) { return a >= b; },
    '+': sum,
    '-': subtract,
    '*': product,
    '/': divide,
    'inc': function (a) { return a + 1; },
    "time-ms": time_ms,
    'max': max,
    'min': min,
    'range': range,
    'sort': sort,
    'peek': peek,
    'pop': pop,
    'lower-case': lowerCase,
    'upper-case': upperCase,
    'str/lower-case': lowerCase,
    'str/upper-case': upperCase,
    //'isletter': isLetter,
    'subs': _substring,
    'subvec': _subvec,

    'list': types._list,
    'list?': types._list_Q,
    'vector': types._vector,
    'vector?': types._vector_Q,
    'hash-map': types._hash_map,
    'map?': types._hash_map_Q,
    'assoc': assoc,
    'dissoc': dissoc,
    'get': get,
    're-seq': re_seq,
    'contains?': contains_Q,
    'keys': keys,
    'vals': vals,
    'int': int,
    //'mod': mod,
    'rem': mod,
    'iterate': iterate,
    'walk': walk,

    'sequential?': types._sequential_Q,
    'cons': cons,
    'concat': concat,
    'vec': vec,
    'nth': nth,
    'first': first,
    'second': second,
    'rest': rest,
    'last': last,
    'take': take,
    'drop': drop,
    'empty?': empty_Q,
    'count': count,
    'apply*': apply,
    //'map': map,
    'repeat': repeat,
    'join': _join,
    'str/join': _join,
    'str/replace': _replace,

    'conj': conj,
    'seq': seq,
    'filter': filter,

    'with-meta': with_meta,
    'meta': meta,
    'atom': types._atom,
    'atom?': types._atom_Q,
    "deref": deref,
    "reset!": reset_BANG,
    "swap!": swap_BANG,

    'js-eval': js_eval,
    '.': js_method_call,

    'set': toSet,
    'disj': _disj,
    'set/union': _union,
    'set/intersection': _intersection,
    'set/symmetric-difference': symmetricDifference,
    'set/difference': _difference
};