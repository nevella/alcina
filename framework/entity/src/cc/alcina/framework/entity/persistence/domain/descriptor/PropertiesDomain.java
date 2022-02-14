package cc.alcina.framework.entity.persistence.domain.descriptor;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.entity.PersistentSingleton;
import cc.alcina.framework.common.client.lock.Lockable;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.UserProperty;
import cc.alcina.framework.common.client.logic.domain.UserPropertyPersistable;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.PermissionsManager;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.entity.persistence.domain.LockUtils;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;

@Registration.Singleton
public class PropertiesDomain {
	public static PropertiesDomain get() {
		return Registry.impl(PropertiesDomain.class);
	}

	private Logger logger = LoggerFactory.getLogger(getClass());

	public <P extends UserPropertyPersistable> P getProperties(Class<P> clazz) {
		IUser user = PersistentSingleton.class.isAssignableFrom(clazz)
				? new UserlandProvider().getSystemUser()
				: PermissionsManager.get().getUser();
		Optional<UserProperty> byUserClass = UserProperty.byUserClass(user,
				clazz);
		if (!byUserClass.isPresent()) {
			Transaction.commit();
			Lockable lock = Locks.get().getUserLock(user);
			try {
				lock.acquire();
				Transaction.commit();
				// double-checked
				byUserClass = UserProperty.byUserClass(user, clazz);
				if (!byUserClass.isPresent()) {
					logger.info("Creating property - {} {}", clazz, user);
					P instance = Reflections.newInstance(clazz);
					UserProperty property = UserProperty.ensure(user,
							clazz.getName());
					property.ensureUserPropertySupport()
							.setPersistable(instance);
					byUserClass = Optional.of(property);
					Transaction.commit();
				}
			} finally {
				lock.release();
			}
		}
		return (P) byUserClass.get().ensureUserPropertySupport()
				.getPersistable();
	}

	@Registration.Singleton
	public static class Locks {
		public static PropertiesDomain.Locks get() {
			return Registry.impl(PropertiesDomain.Locks.class);
		}

		public Lockable getUserLock(IUser user) {
			String path = ((Entity) user).toStringEntity();
			return LockUtils.obtainStringKeyLock(path);
		}
	}
}
