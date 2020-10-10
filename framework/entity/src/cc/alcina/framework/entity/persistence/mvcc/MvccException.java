package cc.alcina.framework.entity.persistence.mvcc;

public class MvccException extends RuntimeException {
	public MvccException() {
		super();
	}

	public MvccException(String message) {
		super(message);
	}

	public MvccException(String message, Throwable cause) {
		super(message, cause);
	}

	public MvccException(Throwable cause) {
		super(cause);
	}
}
