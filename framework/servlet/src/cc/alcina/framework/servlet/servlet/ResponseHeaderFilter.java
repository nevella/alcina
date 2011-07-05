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

package cc.alcina.framework.servlet.servlet;import java.io.IOException;
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
/**
 *
 * @author Nick Reddel
 */

 public class ResponseHeaderFilter implements Filter {	FilterConfig fc;	public static final String URL_REGEX = "Url-Regex";	public void doFilter(ServletRequest req, ServletResponse res,			FilterChain chain) throws IOException, ServletException {		String filterRegex = null;		Map<String, String> headerKvs = new LinkedHashMap<String, String>();		for (Enumeration e = fc.getInitParameterNames(); e.hasMoreElements();) {			String k = (String) e.nextElement();			String v = fc.getInitParameter(k);			if (URL_REGEX.equals(k)) {				filterRegex = v;			} else {				headerKvs.put(k, v);			}		}			HttpServletResponse response = (HttpServletResponse) res;			HttpServletRequest request = (HttpServletRequest) req;		boolean addHeaders = filterRegex == null;		if (!addHeaders) {			addHeaders=Pattern.matches(filterRegex, request.getRequestURI());		}		if (addHeaders) {			for (String k : headerKvs.keySet()) {				response.addHeader(k, headerKvs.get(k));			}		}		chain.doFilter(req, response);	}	public void init(FilterConfig filterConfig) {		this.fc = filterConfig;	}	public void destroy() {		this.fc = null;	}}