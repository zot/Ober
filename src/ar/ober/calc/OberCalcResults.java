// Created on Nov 22, 2003 by Bill Burdick
package ar.ober.calc;

public interface OberCalcResults {
	void replace(int start, int end, String str);
	void noChange(int start, int end);
	void variable(int i, int j);
}
