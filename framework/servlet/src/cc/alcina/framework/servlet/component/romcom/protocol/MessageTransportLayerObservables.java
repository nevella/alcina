package cc.alcina.framework.servlet.component.romcom.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.process.ProcessObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageToken;

public class MessageTransportLayerObservables {
	public static class SentObservable implements ProcessObservable {
		public MessageToken message;

		public boolean resending;

		public SentObservable(MessageToken message, boolean resending) {
			this.message = message;
			this.resending = resending;
		}

		@Reflected
		static class LogObserver implements ProcessObserver<SentObservable> {
			@Override
			public void topicPublished(SentObservable message) {
				if (message.message.message instanceof RemoteComponentProtocol.Message.Mutations) {
					logger.debug("{} {} - mutations sent", Ax.appMillis(),
							message.message.transportHistory.messageId);
				}
			}
		}
	}

	public static class ReceivedObservable implements ProcessObservable {
		public MessageToken message;

		public ReceivedObservable(MessageToken message) {
			this.message = message;
		}
	}

	public static class RetryObservable implements ProcessObservable {
		public MessageToken message;

		public RetryObservable(MessageToken message) {
			this.message = message;
		}
	}

	public static class PublishedObservable implements ProcessObservable {
		public MessageToken message;

		public PublishedObservable(MessageToken message) {
			this.message = message;
		}

		@Reflected
		static class LogObserver
				implements ProcessObserver<PublishedObservable> {
			@Override
			public void topicPublished(PublishedObservable message) {
				if (message.message.message instanceof RemoteComponentProtocol.Message.Mutations) {
					logger.debug("{} {} - mutations published - {}",
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
