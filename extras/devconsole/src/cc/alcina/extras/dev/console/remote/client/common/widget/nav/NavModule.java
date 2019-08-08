package cc.alcina.extras.dev.console.remote.client.common.widget.nav;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.gwt.client.lux.LuxModule;

public class NavModule {
    private static NavModule instance;

    public static NavModule get() {
        if (instance == null) {
            instance = new NavModule();
        }
        return instance;
    }

    public NavResources resources = GWT.create(NavResources.class);

    private NavModule() {
        LuxModule luxModule = LuxModule.get();
        luxModule.interpolateAndInject(resources.navStyles());
        luxModule.interpolateAndInject(resources.navStylesCenter());
        luxModule.interpolateAndInject(resources.navStylesCenterActionButton());
        luxModule.interpolateAndInject(resources.navStylesCenterMenuButton());
        luxModule.interpolateAndInject(resources.navStylesCenterSearch());
        luxModule.interpolateAndInject(resources.navPopup());
    }
}
