package cc.alcina.framework.common.client.logic.domaintransform;

public class UnrecognizedDomainClassException extends RuntimeException {
	public UnrecognizedDomainClassException(Class clazz) {
		super("Unhandled domain class - " + clazz.getName());
	}
}