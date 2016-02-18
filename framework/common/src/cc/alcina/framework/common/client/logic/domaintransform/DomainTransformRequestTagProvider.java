package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;

@RegistryLocation(registryPoint = DomainTransformRequestTagProvider.class)
public class DomainTransformRequestTagProvider {
	public static final transient String CONTEXT_COMMIT_REQUEST_TAG = DomainTransformRequestTagProvider.class
			.getName() + ".CONTEXT_COMMIT_REQUEST_TAG";

	protected DomainTransformRequestTagProvider() {
	}

	private String tag;

	public static DomainTransformRequestTagProvider get() {
		DomainTransformRequestTagProvider singleton = Registry
				.checkSingleton(DomainTransformRequestTagProvider.class);
		if (singleton == null) {
			singleton = new DomainTransformRequestTagProvider();
			Registry.registerSingleton(DomainTransformRequestTagProvider.class,
					singleton);
		}
		return singleton;
	}

	public void setTag(String tag) {
		this.tag = tag;
	}

	public String getTag() {
		if (LooseContext.containsKey(CONTEXT_COMMIT_REQUEST_TAG)) {
			return LooseContext.get(CONTEXT_COMMIT_REQUEST_TAG);
		} else {
			return tag;
		}
	}
}
