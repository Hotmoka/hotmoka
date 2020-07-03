package io.hotmoka.xodus;

public class ExodusException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public ExodusException(jetbrains.exodus.ExodusException e) {
		super(e);
	}
}