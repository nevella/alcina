package cc.alcina.extras.dev.console.remote.client.common.logic;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;

import cc.alcina.extras.dev.console.remote.client.module.console.ConsoleActivity;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation.ImplementationType;

@RegistryLocation(registryPoint = ActivityMapper.class, implementationType = ImplementationType.SINGLETON)
@ClientInstantiable
public class RemoteConsoleActivityMapper implements ActivityMapper {
    @Override
    public Activity getActivity(Place place) {
        if (place instanceof ConsolePlace) {
            return new ConsoleActivity((ConsolePlace) place);
        }
        return null;
    }

    public static class ConsolePlace extends Place {
        public long id;
    }
}
