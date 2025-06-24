package cc.alcina.framework.servlet.environment;

import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.servlet.AuthenticationTokenStore;

/**
 * Provides a way for romcom threads (not directly associated with an
 * HttpServletResponse) to send auth cookie deltas to the browser
 */
class RemoteAuthenticationStore implements AuthenticationTokenStore {
	Environment environment;

	AuthenticationTokenStore delegate;

	RemoteAuthenticationStore(Environment environment) {
		this.environment = environment;
	}

	public void addHeader(String name, String value) {
		delegate.addHeader(name, value);
	}

	public String getCookieValue(String cookieName) {
		return delegate.getCookieValue(cookieName);
	}

	public String getHeaderValue(String headerName) {
		return delegate.getHeaderValue(headerName);
	}

	public String getReferrer() {
		return delegate.getReferrer();
	}

	public String getRemoteAddress() {
		return delegate.getRemoteAddress();
	}

	public String getUrl() {
		return delegate.getUrl();
	}

	public String getUserAgent() {
		return delegate.getUserAgent();
	}

	@Override
	public void wrapDelegate(AuthenticationTokenStore delegate) {
		this.delegate = delegate;
	}

	@Override
	public void setCookieValue(String name, String value) {
		environment.access()
				.dispatchToClient(new Message.SetCookieServerSide(name, value));
	}
}
