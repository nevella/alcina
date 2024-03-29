package cc.alcina.framework.servlet.knowns;

import cc.alcina.framework.common.client.logic.reflection.ClearStaticFieldsOnAppShutdown;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registrations;

@Registrations({
		@Registration(
			value = KnownsRegistrar.class,
			implementation = Registration.Implementation.SINGLETON),
		@Registration(ClearStaticFieldsOnAppShutdown.class) })
public abstract class KnownsRegistrar {
	public abstract void register(KnownsPersistence persistence);
}
