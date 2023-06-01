package cc.alcina.framework.entity.gwt.headless;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.user.client.impl.WindowImpl;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

public class GWTBridgeHeadless extends GWTBridge {
	private Set<Class> notImplemented = new LinkedHashSet<>();

	@Override
	public <T> T create(Class<?> classLiteral) {
		Optional<?> optional = Registry.optional(classLiteral);
		if (!optional.isPresent() && notImplemented.add(classLiteral)) {
			Ax.err("No GWT headless implementation: %s", classLiteral);
		}
		return (T) optional.get();
	}

	@Override
	public String getVersion() {
		return null;
	}

	@Override
	public boolean isClient() {
		return false;
	}

	@Override
	public void log(String message, Throwable e) {
		if (message != null) {
			System.out.println(message);
		}
		if (e != null) {
			e.printStackTrace();
		}
	}

	@Registration(WindowImpl.class)
	public static class WindowImplHeadles extends WindowImpl {
		private String hash = "";

		private String queryString = "";

		@Override
		public String getHash() {
			return this.hash;
		}

		@Override
		public String getQueryString() {
			return this.queryString;
		}

		@Override
		public void initWindowCloseHandler() {
			// FIXME - remocom
		}

		@Override
		public void initWindowResizeHandler() {
			// FIXME - remocom
		}

		@Override
		public void initWindowScrollHandler() {
			// FIXME - remocom
		}

		public void setHash(String hash) {
			this.hash = hash;
		}

		public void setQueryString(String queryString) {
			this.queryString = queryString;
		}
	}
}