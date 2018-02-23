package javax.xml.bind.annotation.adapters;

public abstract class XmlAdapter<A, B> {
	@Override
	public abstract A marshal(B b) throws Exception;

	@Override
	public abstract B unmarshal(A a) throws Exception ;
}