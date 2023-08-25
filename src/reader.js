function Reader(tokens) {
    this.tokens = tokens.map(function (a) { return a; });
    this.position = 0;
}
Reader.prototype.next = function () { return this.tokens[this.position++]; }
Reader.prototype.peek = function () { return this.tokens[this.position]; }