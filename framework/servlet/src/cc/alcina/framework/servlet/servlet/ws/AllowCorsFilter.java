package cc.alcina.framework.servlet.servlet.ws;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

@Provider
public class AllowCorsFilter implements ContainerRequestFilter {
	@Context
	private HttpServletResponse httpResponse;

	@Context
	private HttpServletRequest httpRequest;

	@Override
	public void filter(ContainerRequestContext context) throws IOException {
		Configuration configuration = Configuration.get();
		String explicitRegex = configuration.getAllowExplicitRegex();
		if (explicitRegex != null) {
			String origin = httpRequest.getHeader("origin");
			if (origin.matches(explicitRegex)) {
				httpResponse.addHeader("Access-Control-Allow-Origin", origin);
				return;
			}
		}
		if (configuration.isAllowWildcard()) {
			httpResponse.addHeader("Access-Control-Allow-Origin", "*");
		}
	}

	public static class Configuration {
		public static AllowCorsFilter.Configuration get() {
			return Registry.impl(AllowCorsFilter.Configuration.class);
		}

		private boolean allowWildcard;

		private String allowExplicitRegex;

		public String getAllowExplicitRegex() {
			return this.allowExplicitRegex;
		}

		public boolean isAllowWildcard() {
			return this.allowWildcard;
		}

		public void setAllowExplicitRegex(String allowExplicitRegex) {
			this.allowExplicitRegex = allowExplicitRegex;
		}

		public void setAllowWildcard(boolean allowWildcard) {
			this.allowWildcard = allowWildcard;
		}
	}
}
