package cc.alcina.framework.gwt.client.provider;

import com.google.gwt.dom.client.StyleInjector;

import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentRegistration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;

public interface ResourceLoader {
	public enum Type {
		style, handoff_script
	}

	public enum Priority {
		immediate, deferred
	}

	public static void injectStyle(String cacheKey, String contents,
			Priority priority) {
		if (Al.isBrowser()) {
			StyleInjector.inject(contents);
		} else {
			Registry.impl(RomcomImpl.class).injectStyle(cacheKey, contents,
					priority);
		}
	}

	@EnvironmentRegistration
	@Feature.Ref(Feature_RemoteObjectComponent._Impl.class)
	public interface RomcomImpl {
		void injectStyle(String cacheKey, String contents, Priority priority);
	}
}
