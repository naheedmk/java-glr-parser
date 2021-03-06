package felix.parser.util;

import java.io.File;

public class FileRange {
	public final String filename;
	public final FilePos start;
	private final FilePos end;
	public FileRange(String filename, FilePos start, FilePos end) {
		super();
		if(filename == null) throw new NullPointerException();
		if(start == null) throw new NullPointerException();
		if(end == null) throw new NullPointerException();
		this.filename = filename;
		this.start = start;
		this.end = end;
	}
	public FileRange(FileRange head, FileRange tail) {
		this(head.filename, head.start, tail.getEnd());
		if(!head.filename.equals(tail.filename)) throw new IllegalStateException(); // Don't expect parse trees to span multiple files, do we?
		if(!head.before(tail)) throw new IllegalStateException(); // Don't expect nodes to be out of order in that array
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((getEnd() == null) ? 0 : getEnd().hashCode());
		result = prime * result + ((filename == null) ? 0 : filename.hashCode());
		result = prime * result + ((start == null) ? 0 : start.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileRange other = (FileRange) obj;
		if (!end.equals(other.end))
			return false;
		if (!filename.equals(other.filename))
			return false;
		if (!start.equals(other.start))
			return false;
		return true;
	}
	public boolean before(FileRange tail) {
		return start.before(tail.start);
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(filename);
		sb.append(": ");
		sb.append(start);
		if(!start.equals(getEnd())) {
			sb.append(" to ");
			sb.append(getEnd().toString(start));
		}
		return sb.toString();
	}
	public FilePos getEnd() {
		return end;
	}
	public int getStartOffset() {
		return start.offset;
	}
	public int getEndOffset() {
		return end.offset;
	}
	
	/**
	 * Number of characters between the start and end of the range, including newlines.
	 */
	public int length() {
		return getEndOffset() - getStartOffset();
	}
	
}
