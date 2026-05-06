package cc.alcina.framework.servlet.component.console.rcs;

import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;

@Feature.Ref(Feature_RomcomSessionConsole._Cache.class)
public interface RomcomSessionProvider {
	public static RomcomSessionProvider get() {
		return Registry.impl(RomcomSessionProvider.class);
	}

	Stream<RomcomSessionEntry> getSessions();

	void clear();
}
