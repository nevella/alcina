package cc.alcina.framework.gwt.client.logic;

import java.util.Date;

import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.ReflectionModule;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.util.CurrentUtcDateProvider;

@RegistryLocation(registryPoint = CurrentUtcDateProvider.class, implementationType = ImplementationType.SINGLETON, priority = RegistryLocation.PREFERRED_LIBRARY_PRIORITY)
@ReflectionModule(ReflectionModule.INITIAL)
@ClientInstantiable
public class ClientUTCDateProvider implements CurrentUtcDateProvider {
	@SuppressWarnings("deprecation")
	public Date currentUtcDate() {
		Date d = new Date();
		return new Date(d.getTime() + d.getTimezoneOffset() * 60 * 1000);
	}
}
