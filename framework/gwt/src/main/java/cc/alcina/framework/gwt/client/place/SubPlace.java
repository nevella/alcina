package cc.alcina.framework.gwt.client.place;

public interface SubPlace<E extends Enum> {
	E getSub();

	String toTokenString();
}
