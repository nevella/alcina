package cc.alcina.framework.servlet.job;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;

public interface JobEnvironment {
	void commit();

	ClientInstance getPerformerInstance();
}
