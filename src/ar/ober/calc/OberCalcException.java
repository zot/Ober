// Created on Nov 22, 2003 by Bill Burdick
package ar.ober.calc;

public class OberCalcException extends RuntimeException {
	public OberCalcVariable var;

	public OberCalcException(OberCalcVariable var, String message) {
		super(message);
		this.var = var;
	}
}
