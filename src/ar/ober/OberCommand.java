/*
(C) 2003 Bill Burdick

ar.ober.OberCommand

This software is distributed under the terms of the
Artistic License. Read the included file
License.txt for more information.
*/
package ar.ober;

public abstract class OberCommand {
	public abstract void execute(OberContext ctx);
	
	protected String description;

	public OberCommand(String str) {
		description = str;
	}
	public String getDescription() {
		return description;
	}
}
