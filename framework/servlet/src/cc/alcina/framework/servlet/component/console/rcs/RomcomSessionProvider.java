package cc.alcina.framework.servlet.component.console.rcs;

import java.util.Objects;
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

	void persist(RomcomSessionEntry romcomSessionEntry);

	String ensureSession(String path);

	default RomcomSessionEntry getSession(String path) {
		return getSessions().filter(s -> Objects.equals(path, s.path))
				.findFirst().orElse(null);
	}
}
