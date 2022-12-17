/*
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package cc.alcina.framework.servlet.servlet;

import java.io.IOException;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import cc.alcina.framework.common.client.util.Ax;

/**
 *
 * @author Nick Reddel
 */
public class ResponseHeaderFilter implements Filter {
	public static final String URL_REGEX = "Url-Regex";

	public static final String CROSS_ORIGIN_IF_GWT_CODESERVER = "Cross-Origin-If-GWT-CodeServer";

	FilterConfig fc;

	private Pattern filterRegex;

	private Map<String, String> headerKvs;

	private boolean crossOriginIfGwtCodeserver;

	@Override
	public void destroy() {
		this.fc = null;
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res,
			FilterChain chain) throws IOException, ServletException {
		HttpServletResponse response = (HttpServletResponse) res;
		HttpServletRequest request = (HttpServletRequest) req;
		boolean addHeaders = filterRegex == null
				|| filterRegex.matcher(request.getRequestURI()).matches();
		if (addHeaders) {
			for (String k : headerKvs.keySet()) {
				response.addHeader(k, headerKvs.get(k));
			}
		}
		if (crossOriginIfGwtCodeserver) {
			boolean usesCodeserver = (Ax.notBlank(request.getQueryString())
					&& request.getParameter("gwt.codesvr") != null)
					|| Ax.matches(request.getHeader("Referer"),
							".*gwt.codesvr.*");
			if (usesCodeserver) {
				response.addHeader("Cross-Origin-Embedder-Policy",
						"require-corp");
				response.addHeader("Cross-Origin-Opener-Policy", "same-origin");
			}
		}
		chain.doFilter(req, response);
	}

	@Override
	public void init(FilterConfig filterConfig) {
		this.fc = filterConfig;
		filterRegex = null;
		headerKvs = new LinkedHashMap<String, String>();
		for (Enumeration e = fc.getInitParameterNames(); e.hasMoreElements();) {
			String k = (String) e.nextElement();
			String v = fc.getInitParameter(k);
			switch (k) {
			case URL_REGEX:
				filterRegex = Pattern.compile(v);
				break;
			case CROSS_ORIGIN_IF_GWT_CODESERVER:
				crossOriginIfGwtCodeserver = Boolean.valueOf(v);
				break;
			default:
				headerKvs.put(k, v);
				break;
			}
		}
	}
}