// Created on Nov 22, 2003 by Bill Burdick
package ar.ober.calc;

public abstract class OberCalcVariable {
	public abstract void appendTo(StringBuffer buf, int level);

	public int length;
	public int start;
	public int nameStart;
	public String name;
	public String source;

	public OberCalcVariable(int nameStart, String name, int start, int end, String docText) {
		this.name = name;
		this.start = start;
		this.length = end - start;
		this.nameStart = nameStart;
		source = docText.substring(start, end);
	}
	public String format(String value) {
		return value;
	}
}
