package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.behavior.BehaviorRegistry;
import com.google.gwt.dom.client.mutations.LocalMutations;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.history.push.HistoryImplDelegate;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.util.TimerGwt;
import cc.alcina.framework.servlet.component.Feature_RemoteObjectComponent;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.ActiveMessagesChanged;
import cc.alcina.framework.servlet.component.romcom.protocol.MessageTransportLayer.MessageToken;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.AwaitRemote;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.EnvironmentInitComplete.EnvironmentSettings;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ProcessingException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.Startup;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentWindowProperties;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer;

/*
 * Note that the protocol is stateless - so caller context is dropped (just
 * respond to whatever the server sends back)
 */
public class RemoteComponentUi {
	class PreviewEventRouter implements NativePreviewHandler {
		@Override
		public void onPreviewNativeEvent(NativePreviewEvent event) {
			Event nativeEvent = (Event) event.getNativeEvent();
			ClientEventDispatch.dispatchEventMessage(nativeEvent, null, true);
		}
	}

	/**
	 * Transforms some rpc messages observables into UI mods (for testing,
	 * long-running message processing indication)
	 */
	@Feature.Ref(Feature_RemoteObjectComponent.Feature_ClientMessageState.class)
	class MessageStateRouter {
		void onMessageHandlingException(Message message, Throwable e) {
			// FIXME - ask the context to log
			Ax.out("Exception handling message %s\n"
					+ "================\nSerialized form:\n%s", message, "??");
			e.printStackTrace();
			/*
			 * FIXME - devex - 0 - once syncmutations.3 is stable, this should
			 * not occur (ha!)
			 *
			 * Serious, the romcom client is a bounded piece of code that just
			 * propagates server changes to the client dom, so all exceptions
			 * *should* be server-only (unless client dom is mashed by an
			 * extension)
			 */
			Window.alert(CommonUtils.toSimpleExceptionMessage(e));
		}

		Element notificationElement;

		void displayRpcStateMessage(String message) {
			notificationElement = Document.get()
					.createElement("romcom-notification");
			notificationElement.setTextContent(message);
			Document.get().getBody().appendChild(notificationElement);
			LocalDom.flush();
			String display = notificationElement.implAccess().ensureJsoRemote()
					.getComputedStyle().getDisplay();
			if (Objects.equals(display, "inline")) {
				// not set by the app, add our own
				notificationElement.setAttribute("style",
						"position: absolute; top:5px; left: 5px; padding: 0.5rem 1rem; "
								+ "display: block; background-color: #333; border: solid 1px #ccc; color: #cc5; z-index: 999");
			}
		}

		void clearRpcStateMessage() {
			if (notificationElement != null) {
				notificationElement.removeFromParent();
				notificationElement = null;
			}
		}

		MessageStateRouter() {
			new MessageStateObserver().bind();
		}

		boolean showingLongProcessing;

		@Reflected
		class MessageStateObserver implements
				ProcessObserver<MessageTransportLayer.ActiveMessagesChanged> {
			Timer checkAgedTimer;

			@Override
			public void topicPublished(ActiveMessagesChanged message) {
				List<MessageToken> activeMessages = ClientRpc.get()
						.getActiveMessages();
				boolean hasActiveNonAwaitRemote = activeMessages.stream()
						.anyMatch(
								token -> !(token.message instanceof AwaitRemote));
				Window.setOrRemoveProperty(
						RemoteComponentWindowProperties.INFLIGHT_NON_AWAIT_MESSAGE,
						hasActiveNonAwaitRemote);
				if (hasActiveNonAwaitRemote) {
					if (checkAgedTimer == null) {
						checkAgedTimer = Timer.Provider.get()
								.getTimer(this::checkAged);
						checkAgedTimer.scheduleRepeating(100);
					}
				} else {
					if (checkAgedTimer != null) {
						checkAged();
						checkAgedTimer.cancel();
						checkAgedTimer = null;
					}
				}
			}

			void checkAged() {
				List<MessageToken> activeMessages = ClientRpc.get()
						.getActiveMessages();
				Date oldestActiveNonAwaitRemote = activeMessages.stream()
						.filter(token -> !(token.message instanceof AwaitRemote))
						.map(token -> token.transportHistory.sent)
						.filter(Objects::nonNull).sorted().findFirst()
						.orElse(null);
				boolean longProcessing = oldestActiveNonAwaitRemote != null
						&& !TimeConstants.within(
								oldestActiveNonAwaitRemote.getTime(),
								environmentSettings.longRunningMessageTimeMs);
				if (showingLongProcessing != longProcessing) {
					if (showingLongProcessing) {
						clearRpcStateMessage();
					} else {
						displayRpcStateMessage("processing [please wait]...");
					}
					showingLongProcessing = longProcessing;
				}
			}
		}
	}

	PreviewEventRouter previewEventRouter;

	MessageStateRouter messageStateRouter;

	EnvironmentSettings environmentSettings = new EnvironmentSettings();

	List<Element> offsetObservedElements = new ArrayList<>();

	public void init() {
		previewEventRouter = new PreviewEventRouter();
		messageStateRouter = new MessageStateRouter();
		Event.addNativePreviewHandler(previewEventRouter);
		BehaviorRegistry.get().init(true);
		initWindowListeners();
		Registry.register().singleton(Timer.Provider.class,
				new TimerGwt.Provider());
		Registry.register().singleton(ClientRpc.class, new ClientRpc(this));
		HistoryImplDelegate.pushStateEnabled = Boolean
				.valueOf(ClientUtils.wndString(
						RemoteComponentProtocolServer.ROMCOM_HISTORY_PUSHSTATE));
		History.addValueChangeHandler(hash -> {
			if (!RemoteObjectModelComponentState.get().firingLocationMutation) {
				ClientRpc.send(Message.Mutations.ofLocation());
			}
		});
		/*
		 * FIXME - romcom - in general exception handling is too suppressive
		 * (unexpected exceptions are not bubbled to the message processing
		 * code.
		 * 
		 * LocalDom exceptions are a special case - they're part of a process,
		 * and it's better to best-effort continue that process (mutation
		 * application) rather than bail - hence _they're_ handled vaguely
		 * correctly, the general case is not
		 */
		LocalDom.topicPublishException().add(this::onLocalDomException);
		LocalDom.getLocalMutations().topicBatchedMutations
				.add(this::onLocalMutations);
		LocalDom.getLocalMutations().topicBehaviorAdded
				.add(this::onBehaviorAdded);
		ClientRpc.get().transportLayer.session = ReflectiveSerializer
				.deserializeRpc(ClientUtils.wndString(
						RemoteComponentProtocolServer.ROMCOM_SERIALIZED_SESSION_KEY));
		Startup startupMessage = Message.Startup.forClient();
		ClientRpc.send(startupMessage);
		/*
		 * Activate devmode transport logging
		 */
		// AlcinaLogUtils.setLogLevelClient(
		// "cc.alcina.framework.servlet.component.romcom", Level.ALL);
	}

	/**
	 * <p>
	 * Most mutations of browser dom are either upstream (Environment/Romcom
	 * server) of 'downstream' (user modification of a ContentEditable).
	 * <p>
	 * But there's a _small_ window of this-app-modifying-dom behaviour:
	 * AttributeBehaviorHandler. This class propagates those changes back to the
	 * server
	 */
	void onLocalMutations(List<MutationRecord> mutationRecords) {
		mutationRecords = mutationRecords.stream().filter(mr -> !mr.hasFlag(
				MutationRecord.FlagApplyingDetachedMutationsToLocalDom.class))
				.toList();
		if (mutationRecords.isEmpty()) {
			return;
		}
		mutationRecords.forEach(mr -> mr.populateAttachIds(true));
		Message.Mutations mutations = new Message.Mutations();
		mutations.domMutations = mutationRecords;
		ClientRpc.send(mutations);
	}

	void onBehaviorAdded(LocalMutations.BehaviorAdded behaviorAdded) {
		if (behaviorAdded.behavior == RemoteElementBehaviors.ElementOffsetsRequired.class) {
			offsetObservedElements.add(behaviorAdded.element);
		}
	}

	void onLocalDomException(Exception exception) {
		ProcessingException processingException = RemoteComponentProtocol.Message.ProcessingException
				.wrap(exception, true);
		ClientRpc.send(processingException);
	}

	// FIXME - dirndl - this can just be sent via normal event creation (ehich
	// now handles window events)
	void onPageHideNativeEvent() {
		Event event = new Event(BrowserEvents.PAGEHIDE);
		ClientEventDispatch.dispatchEventMessage(event, null, false);
	}

	final native void initWindowListeners() /*-{
		var _this=this;
		$wnd.onpagehide = $entry(function (evt){
			_this. @cc.alcina.framework.servlet.component.romcom.client.common.logic.RemoteComponentUi::onPageHideNativeEvent()();
		});
		}-*/;
}
