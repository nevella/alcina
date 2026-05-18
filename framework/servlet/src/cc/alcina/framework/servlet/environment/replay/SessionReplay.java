package cc.alcina.framework.servlet.environment.replay;

import java.util.function.Consumer;

import cc.alcina.framework.common.client.WrappedRuntimeException;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.service.InstanceOracle;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.Url;
import cc.alcina.framework.common.client.util.UrlBuilder;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.servlet.component.console.rcs.Feature_RomcomSessionConsole;
import cc.alcina.framework.servlet.component.romcom.protocol.RemoteComponentProtocol.Message;
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

	public Topic<Status> topicStatusChange = Topic.create();

	public static class Status implements Cloneable {
		public int currentEventId;

		public State state = State.pending;

		public Status() {
		}

		protected Status clone() {
			try {
				return (Status) super.clone();
			} catch (Exception e) {
				throw WrappedRuntimeException.wrap(e);
			}
		}
	}

	void updateStatus(Consumer<Status> modifier) {
		Status status = this.status.clone();
		modifier.accept(status);
		this.status = status;
		topicStatusChange.publish(this.status);
	}

	Status status = new Status();

	WebdriverSession session;

	RemoteComponentEvent clientInitEvent;

	Message.Startup startupEvent;

	public enum State {
		pending, replaying, finished;
	}

	public void start() {
		session = WebdriverService.get().createSession();
		loadSequence();
		launchWd();
		loadUrl();
		updateStatus(next -> next.state = State.replaying);
		iterateEvents();
		updateStatus(next -> next.state = State.finished);
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
		session.navigateTo(startupEvent.locationMutation.href + "?gwt.l");
	}

	void launchWd() {
		session.init();
	}
}
