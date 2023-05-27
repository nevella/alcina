package cc.alcina.extras.dev.component.remote.protocol;

import java.util.Date;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.CommonUtils.DateStyle;

@Bean(PropertySource.FIELDS)
public class RemoteComponentRequest {
	private transient static Session clientSession;

	private transient static int clientMessageId;

	public static synchronized RemoteComponentRequest create() {
		Preconditions.checkState(GWT.isClient());
		if (clientSession == null) {
			clientSession = new Session();
			clientSession.clientInstanceUid = Ax.format("%s__%s", CommonUtils
					.formatDate(new Date(), DateStyle.TIMESTAMP_HUMAN),
					Math.random());
			String path = Window.Location.getPath();
			clientSession.environmentId = path.replaceFirst(".+/(.+)/(.+)",
					"$1");
			clientSession.environmentAuth = path.replaceFirst(".+/(.+)/(.+)",
					"$2");
		}
		RemoteComponentRequest request = new RemoteComponentRequest();
		request.session = clientSession;
		request.requestId = ++clientMessageId;
		return request;
	}

	public Session session;

	public int requestId;

	public ProtocolMessage protocolMessage;

	@Bean(PropertySource.FIELDS)
	public static class Session {
		public String clientInstanceUid;

		public String environmentId;

		public String environmentUid;

		public String environmentAuth;
	}
}
