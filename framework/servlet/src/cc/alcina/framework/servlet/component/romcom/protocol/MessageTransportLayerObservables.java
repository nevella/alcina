package cc.alcina.framework.servlet.component.romcom.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.UnacknowledgedMessage;

public class MessageTransportLayerObservables {
	public static class SentObservable implements ProcessObservable {
		public UnacknowledgedMessage message;

		public boolean resending;

		public SentObservable(UnacknowledgedMessage message,
				boolean resending) {
			this.message = message;
			this.resending = resending;
		}

		@Reflected
		static class LogObserver implements ProcessObserver<SentObservable> {
			@Override
			public void topicPublished(SentObservable message) {
				if (message.message.message instanceof RemoteComponentProtocol.Message.Mutations) {
					logger.info("{} {} - mutations sent", Ax.appMillis(),
							message.message.transportHistory.messageId);
				}
			}
		}
	}

	public static class PublishedObservable implements ProcessObservable {
		public UnacknowledgedMessage message;

		public PublishedObservable(UnacknowledgedMessage message) {
			this.message = message;
		}

		@Reflected
		static class LogObserver
				implements ProcessObserver<PublishedObservable> {
			@Override
			public void topicPublished(PublishedObservable message) {
				if (message.message.message instanceof RemoteComponentProtocol.Message.Mutations) {
					logger.info("{} {} - mutations published - {}",
							Ax.appMillis(),
							message.message.transportHistory.messageId,
							Ax.appMillis(
									message.message.transportHistory.published));
				}
			}
		}
	}

	static Logger logger = LoggerFactory
			.getLogger(MessageTransportLayerObservables.class);

	/**
	 * Log mutation events (sent + published) to syslog
	 */
	public static void logMutationEvents() {
		new SentObservable.LogObserver().bind();
		new PublishedObservable.LogObserver().bind();
	}
}
