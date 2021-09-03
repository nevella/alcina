package cc.alcina.framework.common.client.logic.reflection.registry;

public class RegistryException extends RuntimeException {
	private Registry registry;

	public RegistryException() {
		super();
	}

	public RegistryException(Registry registry, String message) {
		super(message);
		this.registry = registry;
	}

	public Registry getRegistry() {
		return this.registry;
	}
}