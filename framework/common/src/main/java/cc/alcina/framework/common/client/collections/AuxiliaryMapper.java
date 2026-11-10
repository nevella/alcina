package cc.alcina.framework.common.client.collections;

@FunctionalInterface
public interface AuxiliaryMapper<A, B> {
	public void map(A a, B b);
}
