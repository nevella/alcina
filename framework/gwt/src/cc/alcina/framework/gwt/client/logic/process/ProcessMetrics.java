package cc.alcina.framework.gwt.client.logic.process;

import com.google.gwt.http.client.RequestBuilder;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.logic.ClientProperties;
import cc.alcina.framework.gwt.client.rpc.AlcinaRpcRequestBuilder;
import cc.alcina.framework.gwt.client.rpc.OutOfBandMessage.OutOfBandMessageHandler;

@Registration.Singleton
public class ProcessMetrics {
	public static final transient String HEADER_RPC_METRIC_ID = "X-ALCINA-RPC-REQUEST-METRIC-ID";

	public static ProcessMetrics get() {
		return Registry.impl(ProcessMetrics.class);
	}

	private int rpcRequestId;

	public void configureRequest(RequestBuilder rb) {
		rb.setHeader(HEADER_RPC_METRIC_ID, String.valueOf(++rpcRequestId));
	}

	public void install() {
		if (ClientProperties.is(getClass(), Key.startup)) {
			// get info from gwt stats
			// log using alcina metrics framework
			/*
			 * sample gwt stats are installed via
			 * /private/var/local/git/jade/client/src/au/com/barnet/jade/public/
			 * static/stats.js
			 *
			 * then LogStoreInterceptors.installStats
			 *
			 * <script type="text/javascript" src="/static/stats.js">
			 */
		}
		if (ClientProperties.is(getClass(), Key.rpc)) {
			AlcinaRpcRequestBuilder.topicPostFlush.add(this::configureRequest);
		}
	}

	public enum Key implements ClientProperties.Key {
		startup, app, rpc
	}

	public static class RpcMessageHandler
			implements OutOfBandMessageHandler<ProcessMetric.Observer> {
		@Override
		public void handle(ProcessMetric.Observer observer) {
			observer.getMetrics().forEach(Ax::out);
		}
	}
}
