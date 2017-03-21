package cc.alcina.framework.gwt.client.place;

public abstract class BaseSubPlace<E extends Enum> extends BasePlace implements SubPlace<E> {
	protected E sub;

	public void setSub(E sub) {
		this.sub = sub;
	}

	@Override
	public E getSub() {
		return sub;
	}

	public BaseSubPlace() {
	}

	public BaseSubPlace(E sub) {
		setSub(sub);
	}
}
