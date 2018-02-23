package javax.xml.bind.annotation.adapters;

public abstract class XmlAdapter<A, B> {
	public abstract A marshal(B b) throws Exception;

	public abstract B unmarshal(A a) throws Exception ;
}