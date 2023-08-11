import { read_str } from './reader.js'
import { _pr_str } from './printer.js'
import { Env } from './env.js'
import * as types from './types.js'
import * as core from './core.js'
import core_clj from './clj/core.clj?raw'

// read
function READ(str) {
    return read_str(str);
}

// eval
function qqLoop(acc, elt) {
    if (types._list_Q(elt) && elt.length
        && types._symbol_Q(elt[0]) && elt[0].value == 'splice-unquote') {
        return [types._symbol("concat"), elt[1], acc];
    } else {
        return [types._symbol("cons"), quasiquote(elt), acc];
    }
}
function quasiquote(ast) {
    if (types._list_Q(ast) && 0 < ast.length
        && types._symbol_Q(ast[0]) && ast[0].value == 'unquote') {
        return ast[1];
    } else if (types._list_Q(ast)) {
        return ast.reduceRight(qqLoop, []);
    } else if (types._vector_Q(ast)) {
        return [types._symbol("vec"), ast.reduceRight(qqLoop, [])];
    } else if (types._symbol_Q(ast) || types._hash_map_Q(ast)) {
        return [types._symbol("quote"), ast];
    } else {
        return ast;
    }
}

function is_macro_call(ast, env) {
    return types._list_Q(ast) &&
        types._symbol_Q(ast[0]) &&
        env.find(ast[0]) &&
        env.get(ast[0])._ismacro_;
}

function macroexpand(ast, env) {
    while (is_macro_call(ast, env)) {
        var mac = env.get(ast[0]);
        ast = mac.apply(mac, ast.slice(1));
    }
    return ast;
}

function eval_ast(ast, env) {
    if (types._symbol_Q(ast)) {
        return env.get(ast);
    } else if (types._list_Q(ast)) {
        return ast.map(function (a) { return EVAL(a, env); });
    } else if (types._vector_Q(ast)) {
        var v = ast.map(function (a) { return EVAL(a, env); });
        v.__isvector__ = true;
        return v;
    } else if (types._hash_map_Q(ast)) {
        var new_hm = {};
        for (var k in ast) {
            new_hm[k] = EVAL(ast[k], env);
        }
        return new_hm;
    } else {
        return ast;
    }
}

export function clearTests() {
    deftests = []
  }

export var deftests = []
var arglist
var fnBody
var isMultiArity

function fnConfig(ast) {
    var a0 = ast[0], a1 = ast[1], a2 = ast[2], a3 = ast[3], a4 = ast[4]
    if (types._string_Q(a2) && types._vector_Q(a3)) {
        arglist = a3
        fnBody = a4
        isMultiArity = false
    }
    if (types._vector_Q(a2)) {
        arglist = a2
        fnBody = a3
        isMultiArity = false
    }
    if (types._string_Q(a2) && types._list_Q(a3)) {
        fnBody = ast.slice(3)
        isMultiArity = true
    }
    if (types._list_Q(a2)) {
        fnBody = ast.slice(2)
        isMultiArity = true
    }
}

function _EVAL(ast, env) {
    while (true) {

        //printer.println("EVAL:", printer._pr_str(ast, true));
        if (!types._list_Q(ast)) {
            return eval_ast(ast, env);
        }

        // apply list
        ast = macroexpand(ast, env);
        if (!types._list_Q(ast)) {
            return eval_ast(ast, env);
        }
        if (ast.length === 0) {
            return ast;
        }

        var a0 = ast[0], a1 = ast[1], a2 = ast[2], a3 = ast[3];
        switch (a0.value) {
            case "ns":
                return null
            case "def":
                var res = EVAL(a2, env);
                return env.set(a1, res);
            case 'deftest':
                var res = EVAL(a2, env);
                env.set(a1, res);
                deftests.push({ test: a1, result: res })
                return EVAL(a2, env)
            case 'testing':
                return EVAL(a2, env)
            case "defn":
            case "defn-":
                fnConfig(ast, env)
                if (isMultiArity) {
                    let arities = []
                    for (let i = 0; i < fnBody.length; i++) {
                        if (types._list_Q(fnBody[i])) {
                            arities.push(fnBody[i])
                        }
                    }
                    for (let i = 0; i < arities.length; i++) {
                        const args = arities[i][0]
                        const body = arities[i][1]
                        let variadic = false
                        for (let i = 0; i < args.length; i++) {
                            if (args[i].value === '&') {
                                variadic = true
                            }
                        }
                        const fn = types._function(EVAL, Env, body, env, args);
                        var fnName
                        if (variadic) {
                            fnName = a1 + "-variadic"
                        } else {
                            fnName = a1 + "-arity-" + args.length
                        }
                        env.set(types._symbol(fnName), fn)
                    }
                    return "#'" + a1 + " defined"
                } else {
                    const fn = types._function(EVAL, Env, fnBody, env, arglist);
                    env.set(types._symbol(a1), fn)
                    return "#'" + a1 + " defined"
                }
            case "let":
                var let_env = new Env(env);
                for (var i = 0; i < a1.length; i += 2) {
                    let_env.set(a1[i], EVAL(a1[i + 1], let_env));
                }
                ast = a2;
                env = let_env;
                break;
            case "quote":
                return a1;
            case "quasiquoteexpand":
                return quasiquote(a1);
            case "quasiquote":
                ast = quasiquote(a1);
                break;
            case 'defmacro':
                var func = types._clone(EVAL(a2, env));
                func._ismacro_ = true;
                return env.set(a1, func);
            case 'macroexpand':
                return macroexpand(a1, env);
            case "try":
                try {
                    return EVAL(a1, env);
                } catch (exc) {
                    if (a2 && a2[0].value === "catch") {
                        if (exc instanceof Error) { exc = exc.message; }
                        return EVAL(a2[2], new Env(env, [a2[1]], [exc]));
                    } else {
                        throw exc;
                    }
                }
            case "do":
                eval_ast(ast.slice(1, -1), env);
                ast = ast[ast.length - 1];
                break;
            case "if":
                var cond = EVAL(a1, env);
                if (cond === null || cond === false) {
                    ast = (typeof a3 !== "undefined") ? a3 : null;
                } else {
                    ast = a2;
                }
                break;
            case "fn":
                return types._function(EVAL, Env, a2, env, a1);
            default:
                const args = eval_ast(ast.slice(1), env)
                const arity = args.length
                var f
                var fSym
                fnName = ast[0].value
                if (Object.keys(env.data).includes(fnName + "-variadic")) {
                    if (Object.keys(env.data).includes(fnName + "-arity-" + arity)) {
                        fSym = types._symbol(ast[0] + "-arity-" + arity)
                    } else {
                        fSym = types._symbol(ast[0] + "-variadic")
                    }
                    f = EVAL(fSym, env)
                } else if (Object.keys(env.data).includes(fnName + "-arity-" + arity)) {
                    fSym = types._symbol(fnName + "-arity-" + arity)
                    f = EVAL(fSym, env)
                } else {
                    fSym = types._symbol(fnName)
                    f = EVAL(fSym, env)
                }
                if (f.__ast__) {
                    ast = f.__ast__;
                    env = f.__gen_env__(args);
                } else {
                    return f.apply(f, args);
                }
        }

    }
}

function EVAL(ast, env) {
    var result = _EVAL(ast, env);
    return (typeof result !== "undefined") ? result : null;
}

// print
function PRINT(exp) {
    return _pr_str(exp, true);
}

export var repl_env = new Env();
export const evalString = function (str) { return PRINT(EVAL(READ(str), repl_env)); };

// core.js: defined using javascript
for (var n in core.ns) { repl_env.set(types._symbol(n), core.ns[n]); }
repl_env.set(types._symbol('eval'), function (ast) {
    return EVAL(ast, repl_env);
});
repl_env.set(types._symbol('*ARGV*'), []);

// load core.clj
evalString("(do " + core_clj + ")")