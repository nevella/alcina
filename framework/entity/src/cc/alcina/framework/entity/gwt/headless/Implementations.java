package cc.alcina.framework.entity.gwt.headless;

import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.UIObject.DebugIdImpl;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry.RegistryFactory;
import cc.alcina.framework.common.client.logic.reflection.Registration;

public class Implementations {

    @RegistryLocation(registryPoint = UIObject.DebugIdImpl.class, priority = RegistryLocation.MANUAL_PRIORITY, implementationType = ImplementationType.FACTORY)
    @ClientInstantiable
    @Registration(value = UIObject.DebugIdImpl.class, priority = Registration.Priority.MANUAL, implementation = Registration.Implementation.FACTORY)
    public static class DebugIdImplFactory implements RegistryFactory<DebugIdImpl> {

        @Override
        public DebugIdImpl impl() {
            return new DebugIdImpl();
        }
    }
}
