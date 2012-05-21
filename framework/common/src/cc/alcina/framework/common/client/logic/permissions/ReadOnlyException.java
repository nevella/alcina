package cc.alcina.framework.common.client.logic.permissions;

public class ReadOnlyException extends RuntimeException {
	public ReadOnlyException() {
	}

	public ReadOnlyException(String message) {
		super(message);
	}
}
