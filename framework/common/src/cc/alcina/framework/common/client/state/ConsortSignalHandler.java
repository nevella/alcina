package cc.alcina.framework.common.client.state;

public interface  ConsortSignalHandler<S> {
	
	S handlesSignal();
	public void signal(Consort consort);
}
