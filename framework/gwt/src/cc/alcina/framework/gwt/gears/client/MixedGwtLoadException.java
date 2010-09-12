package cc.alcina.framework.gwt.gears.client;

public class MixedGwtLoadException extends Exception{

	private  boolean wipeOffline=true;

	public boolean isWipeOffline() {
		return this.wipeOffline;
	}

	public MixedGwtLoadException(String message, boolean wipeOffline) {
		super(message);
		this.wipeOffline = wipeOffline;
	}

	public MixedGwtLoadException(Throwable cause) {
		super(cause);
	}
}
