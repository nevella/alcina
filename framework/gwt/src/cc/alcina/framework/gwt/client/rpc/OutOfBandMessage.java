package cc.alcina.framework.gwt.client.rpc;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.gwt.client.ClientNotifications;
import cc.alcina.framework.gwt.client.ClientNotifications.Level;
import cc.alcina.framework.gwt.client.ClientState;

/*
 * Will be delivered in http header of RPC request
 */
public interface OutOfBandMessage {
	@Bean
	public static class ClientInstanceMessage extends Bindable
			implements OutOfBandMessage {
		private String messageHtml;

		public String getMessageHtml() {
			return this.messageHtml;
		}

		public void setMessageHtml(String messageHtml) {
			this.messageHtml = messageHtml;
		}
	}

	@ClientInstantiable
	@Registration({ OutOfBandMessageHandler.class,
			ClientInstanceMessage.class })
	public static class ClientInstanceMessageHandler
			implements OutOfBandMessageHandler<ClientInstanceMessage> {
		private static boolean shownThisInstance;

		@Override
		public void handle(ClientInstanceMessage outOfBandMessage) {
			if (shownThisInstance) {
			} else {
				shownThisInstance = true;
				ClientNotifications.builder()
						.withCaption(outOfBandMessage.getMessageHtml())
						.withLevel(Level.INFO).withOncePerClientInstance(true)
						.enqueue();
			}
		}
	}

	@Bean
	public static class ExceptionMessage extends Bindable
			implements OutOfBandMessage {
		private String messageHtml;

		public String getMessageHtml() {
			return this.messageHtml;
		}

		public void setMessageHtml(String messageHtml) {
			this.messageHtml = messageHtml;
		}
	}

	@ClientInstantiable
	@Registration({ OutOfBandMessageHandler.class, ExceptionMessage.class })
	public static class ExceptionMessageHandler
			implements OutOfBandMessageHandler<ExceptionMessage> {
		@Override
		public void handle(ExceptionMessage outOfBandMessage) {
			ClientNotifications.get()
					.showWarning(outOfBandMessage.getMessageHtml());
		}
	}

	public interface OutOfBandMessageHandler<T extends OutOfBandMessage> {
		void handle(T outOfBandMessage);
	}

	@Bean
	public static class ReadonlyInstanceMessage extends Bindable
			implements OutOfBandMessage {
		private boolean readonly;

		public boolean isReadonly() {
			return this.readonly;
		}

		public void setReadonly(boolean readonly) {
			this.readonly = readonly;
		}
	}

	@ClientInstantiable
	@Registration({ OutOfBandMessageHandler.class,
			ReadonlyInstanceMessage.class })
	public static class ReadonlyInstanceMessageHandler
			implements OutOfBandMessageHandler<ReadonlyInstanceMessage> {
		@Override
		public void handle(ReadonlyInstanceMessage outOfBandMessage) {
			AlcinaTopics.TOPIC_APP_READONLY
					.publish(outOfBandMessage.isReadonly());
			ClientState.get().setAppReadOnly(outOfBandMessage.isReadonly());
		}
	}
}
