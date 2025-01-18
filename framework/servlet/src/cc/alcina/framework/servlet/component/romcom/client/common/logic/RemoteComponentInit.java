package cc.alcina.framework.servlet.component.romcom.client.common.logic;

import java.util.List;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.AttributeBehaviorHandler.BehaviorRegistry;
import com.google.gwt.dom.client.mutations.MutationRecord;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Event.NativePreviewHandler;
import com.google.gwt.user.client.History;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.common.client.util.Timer;
import cc.alcina.framework.gwt.client.util.ClientUtils;
import cc.alcina.framework.gwt.client.util.TimerGwt;
import cc.alcina.framework.servlet.component.romcom.client.RemoteObjectModelComponentState;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.ProcessingException;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.Startup;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentProtocolServer;

/*
 * Note that the protocol is stateless - so caller context is dropped (just
 * respond to whatever the server sends back)
 */
public class RemoteComponentInit implements NativePreviewHandler {
	public void init() {
		Event.addNativePreviewHandler(this);
		BehaviorRegistry.get().init(true);
		initWindowListeners();
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
		LocalDom.topicMutationsAppliedToLocal()
				.add(this::onMutationsAppliedToLocal);
		ClientRpc.get().transportLayer.session = ReflectiveSerializer
				.deserializeRpc(ClientUtils.wndString(
						RemoteComponentProtocolServer.ROMCOM_SERIALIZED_SESSION_KEY));
		Registry.register().singleton(Timer.Provider.class,
				new TimerGwt.Provider());
		Startup startupMessage = Message.Startup.forClient();
		ClientRpc.send(startupMessage);
		/*
		 * Activate devmode transport logging
		 */
		// AlcinaLogUtils.setLogLevelClient(
		// "cc.alcina.framework.servlet.component.romcom", Level.ALL);
	}

	void onLocalDomException(Exception exception) {
		ProcessingException processingException = RemoteComponentProtocol.Message.ProcessingException
				.wrap(exception);
		ClientRpc.send(processingException);
	}

	void onMutationsAppliedToLocal(List<MutationRecord> mutationRecords) {
		Message.Mutations mutations = new Message.Mutations();
		mutations.domMutations = mutationRecords;
		ClientRpc.send(mutations);
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
			_this. @cc.alcina.framework.servlet.component.romcom.client.common.logic.RemoteComponentInit::onPageHideNativeEvent()();
		});
		}-*/;

	@Override
	public void onPreviewNativeEvent(NativePreviewEvent event) {
		Event nativeEvent = (Event) event.getNativeEvent();
		ClientEventDispatch.dispatchEventMessage(nativeEvent, null, true);
	}
}
