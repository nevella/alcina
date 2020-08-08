package cc.alcina.framework.gwt.client.dirndl.annotation;

public abstract class ActionRef extends Reference {
	public static Class<? extends ActionRef> forId(String token) {
		return Reference.forId(ActionRef.class, token);
	}
}