OberTablePrototype = {}
OberTablePrototype.__proto__ = [].__proto__
OberRowPrototype = {}
OberRowPrototype.__proto__ = OberTablePrototype.__proto__

OberTablePrototype.toString = function () {
	var buf = [];
	for (var i = 0; i < this.length; i++) {
		this[i].addToBuffer(buf);
	}
	return buf.join("");
}

OberRowPrototype.toString = function () {
	var buf = [];
	this.addToBuffer(buf);
	return buf.join("");
}
OberRowPrototype.addToBuffer = function (buf) {
	buf.push(this.realRow.leadingWhitespace);
	buf.push("|");
	for (var i = 0; i < this.length; i++) {
		buf.push(this.realRow.formatValue(i, this[i]));
		buf.push("|");
	}
}
