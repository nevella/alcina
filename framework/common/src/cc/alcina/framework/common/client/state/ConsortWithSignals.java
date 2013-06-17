package cc.alcina.framework.common.client.state;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.state.Consort.TopicListenerOneTimeAsyncCallbackAdapter;
import cc.alcina.framework.common.client.util.TopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

import com.google.gwt.user.client.rpc.AsyncCallback;

public class ConsortWithSignals<D, S> extends Consort<D> {
	Map<S, ConsortSignalHandler<S>> signalHandlers = new LinkedHashMap<S, ConsortSignalHandler<S>>();

	public void addSignalHandler(ConsortSignalHandler<S> signal) {
		if (signalHandlers.containsKey(signal.handlesSignal())) {
			throw new RuntimeException("Duplicate signal handlers for "
					+ signal.handlesSignal());
		}
		signalHandlers.put(signal.handlesSignal(), signal);
	}

	public void signal(S signal, AsyncCallback finishedCallback) {
		signalTopicPublisher.publishTopic(signal.toString(), signal);
		signalHandlers.get(signal).signal(this);
		if (finishedCallback != null) {
			TopicListenerOneTimeAsyncCallbackAdapter adapter = new TopicListenerOneTimeAsyncCallbackAdapter(
					finishedCallback);
			listenerDelta(FINISHED, adapter, true);
			listenerDelta(ERROR, adapter, true);
			listenerDelta(NO_ACTIVE_PLAYERS, adapter, true);
			
		}
	}

	public void signal(S signal) {
		signal(signal, null);
	}

	private TopicPublisher signalTopicPublisher = new TopicPublisher();

	public void signalListenerDelta(String key, TopicListener listener,
			boolean add) {
		signalTopicPublisher.listenerDelta(key, listener, add);
	}
}
