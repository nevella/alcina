package cc.alcina.framework.common.client.util;

import java.util.Date;

import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.RegistrableService;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

/**
 * Interchangable because this (gwt-compat) version is weak compared to real,
 * calendar based implementations
 * 
 * @author nick@alcina.cc
 * 
 */
@RegistryLocation(registryPoint = DateUtils.class, implementationType = ImplementationType.SINGLETON)
public class DateUtils implements RegistrableService {
	private static DateUtils singleton;

	public static DateUtils get() {
		if (singleton == null) {
			singleton = Registry.impl(DateUtils.class);
		}
		return singleton;
	}

	public static int ageInDays(Date date) {
		return get().ageInDays0(date);
	}

	protected int ageInDays0(Date date) {
		return (int) (date == null ? 0 : (System.currentTimeMillis() - date
				.getTime()) / TimeConstants.ONE_DAY_MS);
	}

	@Override
	public void appShutdown() {
		singleton = null;
	}
}
