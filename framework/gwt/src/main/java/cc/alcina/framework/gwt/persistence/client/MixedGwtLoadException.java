package cc.alcina.framework.gwt.persistence.client;

public class MixedGwtLoadException extends RuntimeException {
	private boolean wipeOffline = true;

	public MixedGwtLoadException(String message, boolean wipeOffline) {
		super(message);
		this.wipeOffline = wipeOffline;
	}

	public MixedGwtLoadException(Throwable cause) {
		super(cause);
	}

	public boolean isWipeOffline() {
		return this.wipeOffline;
	}
}
