package org.jwat.tools;

public class ExitException extends SecurityException {

	private static final long serialVersionUID = -1982617086752946683L;

	public final int status;

	public ExitException(int status) {
		super("There is no escape!");
		this.status = status;
	}

}
