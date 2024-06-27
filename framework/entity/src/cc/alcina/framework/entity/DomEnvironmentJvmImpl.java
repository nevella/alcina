package cc.alcina.framework.entity;

import cc.alcina.framework.common.client.dom.DomEnvironment;
import cc.alcina.framework.common.client.logic.reflection.Registration;

@Registration.Singleton(
	value = DomEnvironment.class,
	priority = Registration.Priority.PREFERRED_LIBRARY)
public class DomEnvironmentJvmImpl extends DomEnvironmentJvmBase {
}
