package cc.alcina.framework.gwt.client.dirndl.cmp.status;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ContextObservers;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.gwt.client.dirndl.cmp.status.StatusModel.Priority;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.logic.CallManager;
import cc.alcina.framework.gwt.client.logic.MessageManager;

/*
 * FIXME - dirndl - CallManager conflates non-dismissable notifications
 * (messages) with dismissable (say 'running x' or 'searching'). Fix that by
 * changing the API from String to Notification (or Message) - and then refactor
 * callmanager, messagemanager and the status modules (in fact, the internal
 * refactor is done already done - But need to add 'DISMISS', 'UNDO' etc)
 *
 * dirndl app messsages should come via Notification, which routes to here via
 * (legacy) MessageManager
 *
 */
@Registration.Singleton
@Registration.EnvironmentSingleton
public class StatusModule {
	public static StatusModule get() {
		return Registry.impl(StatusModule.class);
	}

	private StatusModel model;

	public StatusModule() {
		CallManager.topicCallMade
				.add(message -> showMessage(message, Channel.CALL_MADE));
		MessageManager.topicMessagePublished.add(
				message -> showMessage(message, Channel.MESSAGE_PUBLISHED));
		MessageManager.topicNotificationModelPublished.add(
				message -> showMessage(message, Channel.MESSAGE_PUBLISHED));
		MessageManager.topicAppMessagePublished.add(
				message -> showMessage(message, Channel.APP_MESSAGE_PUBLISHED));
		MessageManager.topicIcyMessagePublished.add(
				message -> showMessage(message, Channel.ICY_MESSAGE_PUBLISHED));
		MessageManager.topicCenterMessagePublished
				.add(message -> showMessage(message,
						Channel.CENTER_MESSAGE_PUBLISHED));
		MessageManager.topicIcyCenterMessagePublished
				.add(message -> showMessage(message,
						Channel.ICY_CENTER_MESSAGE_PUBLISHED));
		MessageManager.topicExceptionMessagePublished
				.add(message -> showMessage(message,
						Channel.EXCEPTION_MESSAGE_PUBLISHED));
		new NotificationObserver().bind();
	}

	@Reflected
	class NotificationObserver
			implements ProcessObserver<NotificationObservable> {
		@Override
		public void topicPublished(NotificationObservable message) {
			showMessage(message.message, Channel.MESSAGE_PUBLISHED);
		}
	}

	public DirectedLayout.Rendered getStatusRendered() {
		model = new StatusModel();
		return new DirectedLayout().render(model).getRendered();
	}

	private void showMessage(Model model, Channel channel) {
		ensureModel().addMessage(new Message(model, channel.getPriority()));
	}

	private StatusModel ensureModel() {
		if (this.model == null) {
			getStatusRendered().appendToRoot();
		}
		return this.model;
	}

	private void showMessage(String message, Channel channel) {
		ensureModel().addMessage(new Message(message, channel.getPriority()));
	}

	enum Channel {
		CALL_MADE, CENTER_MESSAGE_PUBLISHED, MESSAGE_PUBLISHED,
		ICY_MESSAGE_PUBLISHED, ICY_CENTER_MESSAGE_PUBLISHED,
		EXCEPTION_MESSAGE_PUBLISHED, APP_MESSAGE_PUBLISHED;

		StatusModel.Priority getPriority() {
			switch (this) {
			case ICY_MESSAGE_PUBLISHED:
			case ICY_CENTER_MESSAGE_PUBLISHED:
			case CENTER_MESSAGE_PUBLISHED:
				return Priority.MAJOR;
			case EXCEPTION_MESSAGE_PUBLISHED:
				return Priority.MAJOR;
			default:
				return Priority.INFO;
			}
		}
	}

	static class Message {
		String string;

		Model model;

		Priority priority;

		Message() {
		}

		Message(Model model, Priority priority) {
			this.model = model;
			this.priority = priority;
		}

		Message(String string, Priority priority) {
			this.string = string;
			this.priority = priority;
		}

		public boolean isNotBlank() {
			return Ax.notBlank(string) || model != null;
		}

		@Override
		public String toString() {
			return FormatBuilder.keyValues("payload",
					Ax.notBlank(string) ? string : model, "priority", priority);
		}
	}

	public void showMessageTransitional(String string) {
		showMessage(string, Channel.MESSAGE_PUBLISHED);
	}
}
