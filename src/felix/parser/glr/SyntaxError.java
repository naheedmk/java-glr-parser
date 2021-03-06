package felix.parser.glr;

import java.text.ParseException;

import felix.parser.util.FileRange;

/**
 * Generic "parse failed" exception.
 */
public class SyntaxError extends ParseException {
	private static final long serialVersionUID = -5848840581016070335L;
	
	
	public final FileRange fileRange;

	public SyntaxError(String message, FileRange fileRange) {
		super(message, fileRange.getEndOffset());
		this.fileRange = fileRange;
	}
	
	
}
