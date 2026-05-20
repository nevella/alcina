package cc.alcina.framework.servlet.environment.replay;

import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DomEventData;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.flight.FlightEvent;
import cc.alcina.framework.common.client.flight.FlightEventWrappable;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.Url;
import cc.alcina.framework.common.client.util.UrlBuilder;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.console.rcs.Feature_RomcomSessionConsole;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message.DomEventMessage;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentEvent;
import cc.alcina.framework.servlet.component.sequence.adapter.FlightEventSequence;
import cc.alcina.framework.servlet.servlet.wd.WebdriverService;
import cc.alcina.framework.servlet.servlet.wd.WebdriverService.WebdriverSession;

@Feature.Ref(Feature_RomcomSessionConsole._Replay.class)
public class SessionReplay {
	SequencePlace sequencePlace;

	FlightEventSequence eventSequence;

	public SessionReplay(SequencePlace sequencePlace) {
		this.sequencePlace = sequencePlace;
	}

	public Topic<State> topicStateChange = Topic.create();

	public static class State implements Cloneable {
		public int currentEventIdx = -1;

		public Phase phase = Phase.pending;

		public String message;

		public State() {
		}

		protected State clone() {
			try {
				return (State) super.clone();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}

	void updateState(Consumer<State> modifier) {
		State state = this.state.clone();
		modifier.accept(state);
		this.state = state;
		topicStateChange.publish(this.state);
	}

	State state = new State();

	WebdriverSession session;

	RemoteComponentEvent clientInitEvent;

	RemoteComponentEvent currentEvent;

	RemoteComponentEvent lastEvent;

	DomEventMessage replayEvents;

	Message lastServerMessage;

	Message.Startup startupEvent;

	public enum Phase {
		pending, replaying, finished, exception;
	}

	public void start() {
		session = WebdriverService.get().createSession();
		loadSequence();
		launchWd();
		loadUrl();
		updateState(next -> next.phase = Phase.replaying);
		try {
			iterateEvents();
		} catch (Exception e) {
			e.printStackTrace();
			updateState(next -> {
				next.phase = Phase.exception;
				next.message = CommonUtils.toSimpleExceptionMessage(e);
			});
			return;
		}
		updateState(next -> next.phase = Phase.finished);
	}

	void loadSequence() {
		InstanceQuery instanceQuery = sequencePlace.instanceQuery;
		InstanceOracle.Query<? extends Sequence> oracleQuery = instanceQuery
				.toOracleQuery();
		eventSequence = (FlightEventSequence) instanceQuery.toOracleQuery()
				.get();
		clientInitEvent = (RemoteComponentEvent) eventSequence.getElements()
				.stream().filter(ev -> ev.event instanceof RemoteComponentEvent)
				.findFirst().get().event;
		startupEvent = (Message.Startup) clientInitEvent.request.messageEnvelope.messages
				.get(0);
	}

	void iterateEvents() {
		for (;;) {
			advanceToNextClientEvent();
			if (state.phase == Phase.finished) {
				break;
			}
			int awaitServerMessageId = lastServerMessage.messageId.number;
			awaitServerMessage(awaitServerMessageId);
			replayClientEvent();
		}
	}

	void replayClientEvent() {
		replayEvents.events.forEach(e -> {
			session.performEvent(e);
			try {
				Thread.sleep(30);
			} catch (Exception e2) {
				throw WrappedRuntimeException.wrap(e2);
			}
		});
	}

	void advanceToNextClientEvent() {
		for (;;) {
			if (state.currentEventIdx == eventSequence.elements.size() - 1) {
				updateState(next -> next.phase = Phase.finished);
				break;
			}
			updateState(next -> next.currentEventIdx++);
			FlightEvent flightEvent = eventSequence.elements
					.get(state.currentEventIdx);
			FlightEventWrappable event = flightEvent.event;
			if (!(event instanceof RemoteComponentEvent)) {
				continue;
			}
			lastEvent = currentEvent;
			if (lastEvent != null) {
				Message lastResponseMessage = Ax
						.last(lastEvent.response.messageEnvelope.messages);
				if (lastResponseMessage != null) {
					this.lastServerMessage = lastResponseMessage;
				}
			}
			currentEvent = (RemoteComponentEvent) event;
			/*
			 * TODO - handle multiple messages
			 */
			Message message = currentEvent.request.messageEnvelope.messages
					.get(0);
			if (message instanceof DomEventMessage) {
				replayEvents = (DomEventMessage) message;
				List<DomEventData> userEvents = replayEvents.events.stream()
						.filter(e -> {
							switch (e.event.getType()) {
							case BrowserEvents.CLICK:
							case BrowserEvents.INPUT:
							case BrowserEvents.CHANGE:
							case BrowserEvents.KEYDOWN:
								return true;
							default:
								return false;
							}
						}).toList();
				if (userEvents.size() == 0) {
					replayEvents = null;
				} else {
					break;
				}
			}
		}
	}

	void awaitServerMessage(int awaitServerMessageId) {
		long timeout = 2000;
		long start = System.currentTimeMillis();
		int replayedId = -1;
		while (TimeConstants.within(start, timeout)) {
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
			String attrValue = session.getDocumentAttribute(
					RemoteComponentProtocol.ATTR_SERVER_MESSAGE_PROCESSED);
			if (Ax.notBlank(attrValue)) {
				replayedId = Integer.parseInt(attrValue);
				if (replayedId >= awaitServerMessageId) {
					return;
				}
			}
		}
		String message = Ax.format(
				"Server replay id not reached - %s; current %s",
				awaitServerMessageId, replayedId);
		Ax.err(message);
		// throw new IllegalStateException(
		// message);
	}

	void loadUrl() {
		/*
		 * call set-client-property to instruct server to set client cookie to
		 * update event attr in dom
		 */
		String sessionHref = startupEvent.locationMutation.href;
		UrlBuilder urlBuilder = Url.parse(sessionHref).toBuilder()
				.clearFromPath();
		urlBuilder.withPath(Ax.format(
				"/set-client-property/RemoteComponentUi/publishProcessedMessageIdBefore/%s",
				System.currentTimeMillis() + 10000));
		session.navigateTo(urlBuilder.build());
		session.navigateTo(startupEvent.locationMutation.href);
	}

	void launchWd() {
		session.init();
	}
}
