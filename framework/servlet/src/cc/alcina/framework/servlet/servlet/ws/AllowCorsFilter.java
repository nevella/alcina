package cc.alcina.framework.servlet.servlet.ws;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;

@PreMatching
@Provider
public class AllowCorsFilter
		implements ContainerRequestFilter, ContainerResponseFilter {
	@Context
	private HttpServletResponse httpResponse;

	@Context
	private HttpServletRequest httpRequest;

	@Override
	public void filter(ContainerRequestContext context) throws IOException {
		if (!CorsFilterConfiguration.has()) {
			// reflectively invoked, ignore
			return;
		}
		CorsFilterConfiguration configuration = CorsFilterConfiguration.get();
		String explicitRegex = configuration.getAllowExplicitRegex();
		if (explicitRegex != null) {
			String origin = httpRequest.getHeader("origin");
			if (Ax.matches(origin, explicitRegex)) {
				httpResponse.addHeader("Access-Control-Allow-Origin", origin);
				return;
			}
		}
		if (configuration.isAllowWildcard()) {
			httpResponse.addHeader("Access-Control-Allow-Origin", "*");
		}
	}

	@Override
	public void filter(ContainerRequestContext arg0,
			ContainerResponseContext arg1) throws IOException {
		// TODO Auto-generated method stub
	}

	public static class CorsFilterConfiguration {
		public static AllowCorsFilter.CorsFilterConfiguration get() {
			return Registry.impl(AllowCorsFilter.CorsFilterConfiguration.class);
		}

		public static boolean has() {
			return Registry
					.optional(AllowCorsFilter.CorsFilterConfiguration.class)
					.isPresent();
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
