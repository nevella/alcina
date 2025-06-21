package cc.alcina.framework.entity.gwt.headless;

import com.google.gwt.storage.client.StorageImpl;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.UIObject.DebugIdImpl;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;

public class Implementations {
	@Reflected
	@Registration(
		value = UIObject.DebugIdImpl.class,
		priority = Registration.Priority.APP,
		implementation = Registration.Implementation.FACTORY)
	public static class DebugIdImplFactory
			implements RegistryFactory<DebugIdImpl> {
		@Override
		public DebugIdImpl impl() {
			return new DebugIdImpl();
		}
	}

	@Reflected
	@Registration(
		value = StorageImpl.class,
		priority = Registration.Priority.APP,
		implementation = Registration.Implementation.FACTORY)
	public static class StorageImplFactory
			implements RegistryFactory<StorageImpl> {
		@Override
		public StorageImpl impl() {
			return new StorageImplHeadless();
		}

		static class StorageImplHeadless extends StorageImpl {
			StorageImplHeadless() {
				super();
			}
		}
	}
}
