package io.datakernel.tag;

import java.io.IOException;

public class ReplaceException extends Exception {
	public ReplaceException(IOException e) {
		super(e);
	}

	public ReplaceException(String message) {
		super(message);
	}
}
