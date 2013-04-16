package cc.alcina.framework.common.client.collections;

public class IsClassFilter implements CollectionFilter {
	private Class clazz;

	public IsClassFilter(Class clazz) {
		this.clazz = clazz;
	}

	@Override
	public boolean allow(Object o) {
		return o.getClass() == clazz;
	}
}