package cc.alcina.framework.servlet.servlet;

import cc.alcina.framework.common.client.logic.domaintransform.ClientInstance;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.StringMap;
import cc.alcina.framework.entity.persistence.AuthenticationPersistence;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.servlet.authentication.AuthenticationManager;

public interface AuthenticationTokenStore {
	void addHeader(String name, String value);

	String getCookieValue(String cookieName);

	String getHeaderValue(String headerName);

	String getReferrer();

	String getRemoteAddress();

	String getUrl();

	String getUserAgent();

	void setCookieValue(String name, String value);

	default void wrapDelegate(AuthenticationTokenStore existingStore) {
		// noop
	}

	public static class ApiAuthenticationTokenStore
			implements AuthenticationTokenStore {
		private String userAgent;

		private StringMap cookieValues = new StringMap();

		private StringMap headerValues = new StringMap();

		@Override
		public void addHeader(String name, String value) {
			headerValues.put(name, value);
		}

		@Override
		public String getCookieValue(String cookieName) {
			return cookieValues.get(cookieName);
		}

		@Override
		public String getHeaderValue(String headerName) {
			return headerValues.get(headerName);
		}

		@Override
		public String getReferrer() {
			return null;
		}

		@Override
		public String getRemoteAddress() {
			return "0.0.0.0";
		}

		@Override
		public String getUrl() {
			return null;
		}

		@Override
		public String getUserAgent() {
			return userAgent;
		}

		public void initFromClientInstanceValues(Long clientInstanceId,
				Integer clientInstanceAuth) {
			addHeader(
					AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_ID_KEY,
					String.valueOf(clientInstanceId));
			addHeader(
					AlcinaRpcRequestBuilder.REQUEST_HEADER_CLIENT_INSTANCE_AUTH_KEY,
					String.valueOf(clientInstanceAuth));
			if (AuthenticationPersistence.get().validateClientInstance(
					clientInstanceId, clientInstanceAuth)) {
				ClientInstance clientInstance = AuthenticationPersistence.get()
						.getClientInstance(clientInstanceId);
				setCookieValue(AuthenticationManager.COOKIE_NAME_IID,
						clientInstance.getAuthenticationSession().getIid()
								.getInstanceId());
				setCookieValue(AuthenticationManager.COOKIE_NAME_SESSIONID,
						clientInstance.getAuthenticationSession()
								.getSessionId());
				Ax.out("ClientInstance %s, %s validated", clientInstanceId,
						clientInstanceAuth);
			} else {
				Ax.err("ClientInstance %s, %s invalid", clientInstanceId,
						clientInstanceAuth);
			}
			AuthenticationManager.get().initialiseContext(this);
		}

		@Override
		public void setCookieValue(String name, String value) {
			cookieValues.put(name, value);
		}

		public void setUserAgent(String userAgent) {
			this.userAgent = userAgent;
		}
	}
}
