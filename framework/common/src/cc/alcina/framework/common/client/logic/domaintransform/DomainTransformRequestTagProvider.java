package cc.alcina.framework.common.client.logic.domaintransform;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.LooseContext;

@ClientInstantiable
@Registration.Singleton
public class DomainTransformRequestTagProvider {
	public static final transient String CONTEXT_COMMIT_REQUEST_TAG = DomainTransformRequestTagProvider.class
			.getName() + ".CONTEXT_COMMIT_REQUEST_TAG";

	public static synchronized DomainTransformRequestTagProvider get() {
		return Registry.impl(DomainTransformRequestTagProvider.class);
	}

	private String tag;

	public DomainTransformRequestTagProvider() {
	}

	public String getTag() {
		if (LooseContext.containsKey(CONTEXT_COMMIT_REQUEST_TAG)) {
			return LooseContext.get(CONTEXT_COMMIT_REQUEST_TAG);
		} else {
			return tag;
		}
	}

	public void setTag(String tag) {
		this.tag = tag;
	}
}
