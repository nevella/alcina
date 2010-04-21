package cc.alcina.framework.common.client.logic.domaintransform;

public class DomainTransformRequestException extends Exception {
	private DomainTransformResponse domainTransformResponse;

	public DomainTransformRequestException() {
	}

	public DomainTransformRequestException(
			DomainTransformResponse domainTransformResponse) {
		super();
		this.domainTransformResponse = domainTransformResponse;
	}

	public DomainTransformRequestException(String message) {
		super(message);
		domainTransformResponse = null;
	}

	public DomainTransformResponse getDomainTransformResponse() {
		return domainTransformResponse;
	}

	@Override
	public String toString() {
		return domainTransformResponse.toExceptionString();
	}
}
