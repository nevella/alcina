package cc.alcina.framework.common.client.consort;

import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.Topic;

public class ConsortWithSignals<D, S> extends Consort<D> {
	Map<S, ConsortSignalHandler<S>> signalHandlers = new LinkedHashMap<S, ConsortSignalHandler<S>>();

	public final Topic<S> topicSignal = Topic.create();

	public void addSignalHandler(ConsortSignalHandler<S> signal) {
		if (signalHandlers.containsKey(signal.handlesSignal())) {
			throw new RuntimeException(
					"Duplicate signal handlers for " + signal.handlesSignal());
		}
		signalHandlers.put(signal.handlesSignal(), signal);
	}

	public void signal(S signal) {
		signal(signal, null);
	}

	public void signal(S signal, AsyncCallback finishedCallback) {
		logger.info(Ax.format("%s%s%s -> %s", "[SG] ",
				CommonUtils.padStringLeft("", depth(), "    "),
				CommonUtils.simpleClassName(getClass()), signal));
		topicSignal.publish(signal);
		signalHandlers.get(signal).signal(this, finishedCallback);
	}
}
