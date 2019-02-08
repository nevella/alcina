package cc.alcina.extras.dev.console.remote.client.common.resources;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import cc.alcina.framework.gwt.client.lux.LuxModule;

public class RemoteConsoleModule {
    private static RemoteConsoleModule indexModule;

    public static RemoteConsoleModule get() {
        if (indexModule == null) {
            indexModule = new RemoteConsoleModule();
        }
        return indexModule;
    }

    public RemoteConsoleResources resources = GWT
            .create(RemoteConsoleResources.class);

    Map<String, String> variableDefs = new LinkedHashMap<>();

    private RemoteConsoleModule() {
        String remoteConsoleTheme = resources.remoteConsoleThemeStyles()
                .getText();
        LuxModule luxModule = LuxModule.get();
        luxModule.setVariables(remoteConsoleTheme);
        luxModule.interpolateAndInject(resources.fontStyles());
        luxModule.interpolateAndInject(resources.remoteConsoleStyles());
    }
}
