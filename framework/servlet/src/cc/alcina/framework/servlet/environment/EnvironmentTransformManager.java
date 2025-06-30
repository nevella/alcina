package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.common.client.logic.domaintransform.ClientTransformManager.ClientTransformManagerCommon;

/**
 * A single-threaded transform manager for use in full-isolated-transform remote
 * clients (which emulate gwt clients)
 */
public class EnvironmentTransformManager extends ClientTransformManagerCommon {
	@Override
	public synchronized long nextLocalIdCounter() {
		return getBootstrapNextLocalId();
	}
}
