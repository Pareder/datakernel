package datakernel.tag;

import java.io.IOException;

public class ReplaceException extends Exception {
	public ReplaceException(IOException e) {
		super(e);
	}
}
