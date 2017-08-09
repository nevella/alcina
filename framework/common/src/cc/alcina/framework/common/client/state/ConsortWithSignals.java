package cc.alcina.framework.common.client.state;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TopicPublisher;
import cc.alcina.framework.common.client.util.TopicPublisher.TopicListener;

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
		infoLogger.log(CommonUtils.formatJ("%s%s%s -> %s", "[SG] ",
				CommonUtils.padStringLeft("", depth(), "    "),
				CommonUtils.simpleClassName(getClass()), signal));
		signalTopicPublisher.publishTopic(signal.toString(), signal);
		signalHandlers.get(signal).signal(this, finishedCallback);
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
