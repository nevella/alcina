package cc.alcina.framework.common.client.logic.domaintransform;

public interface DomainTransformExceptionFilter {
	public boolean ignore(DomainTransformException exception);
}
