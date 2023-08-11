import { read_str } from './reader.js'
import { Env } from './env.js'

// read
function READ(str) {
    return read_str(str);
}

// eval
function EVAL(ast, env) {
    return ast;
}

// print
function PRINT(exp) {
    return exp;
}

export var repl_env = new Env();
export const evalString = function (str) { return PRINT(EVAL(READ(str), repl_env)); };
