package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.util.StringMap;

public interface AuthenticationTokenStore {
	String getCookieValue(String cookieName);

	String getHeaderValue(String headerName);

	String getRemoteAddress();

	String getUserAgent();

	void setCookieValue(String name, String value);

	public static class ApiAuthenticationTokenStore
			implements AuthenticationTokenStore {
		private String userAgent;

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}
		private StringMap cookieValues=new StringMap();
		
		private StringMap headerValues=new StringMap();


		@Override
		public String getCookieValue(String cookieName) {
			return cookieValues.get(cookieName);
		}

		@Override
		public String getHeaderValue(String headerName) {
			return headerValues.get(headerName);
		}

		@Override
		public String getRemoteAddress() {
			return "0.0.0.0";
		}

		@Override
		public String getUserAgent() {
			return userAgent;
		}

		@Override
		public void setCookieValue(String name, String value) {
			cookieValues.put(name, value);
		}@Override
		public void addHeader(String name,
				String value) {
			headerValues.put(name, value);			
		}

		@Override
		public String getUrl() {
			return null;
		}

		@Override
		public String getReferrer() {
			return null;
		}
	}

	void addHeader(String name, String value);

	String getUrl();

	String getReferrer();
}
