package cc.alcina.framework.servlet.component.console.rcs;

import java.util.ArrayList;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Heading;
import cc.alcina.framework.gwt.client.dirndl.model.KeyValue;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.gwt.client.module.support.login.LoginPage.HeadingArea;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.servlet.component.console.ServerConsoleContents;
import cc.alcina.framework.servlet.component.sequence.SequenceComponentEditor;
import cc.alcina.framework.servlet.environment.replay.SessionReplay;
import cc.alcina.framework.servlet.environment.replay.SessionReplay.State;
import cc.alcina.framework.servlet.environment.replay.SessionReplay.Status;

@Feature.Ref(Feature_RomcomSessionConsole._Replay.class)
@Registration({ ServerConsoleContents.class, RomcomSessionDetailPlace.class })
@TypedProperties
class RomcomSessionDetailArea
		extends ServerConsoleContents<RomcomSessionDetailPlace>
		implements SequenceComponentEditor.DefinitionChanged.Binding {
	PackageProperties._RomcomSessionDetailArea.InstanceProperties properties() {
		return PackageProperties.romcomSessionDetailArea.instance(this);
	}

	HeadingArea heading = new HeadingArea("Romcom session detail", null);

	SessionMetadata sequenceMetadata;

	ReplayArea replayArea = new ReplayArea();

	SequenceComponentEditor sequence;

	@Property.Not
	RomcomSessionEntry entry;

	@Directed(tag = "metadata")
	class SessionMetadata extends Model.All {
		List<KeyValue> keyValues = new ArrayList<>();

		SessionMetadata() {
			KeyValue.stringValue("Path", entry.path).addTo(keyValues);
			KeyValue.stringValue("End", entry.end).addTo(keyValues);
			KeyValue.stringValue("Marked", entry.marked).addTo(keyValues);
			KeyValue.stringValue("Exception", entry.exception).addTo(keyValues);
		}
	}

	public static class Replay extends ModelEvent<Object, Replay.Handler> {
		@Override
		public void dispatch(Replay.Handler handler) {
			handler.onReplay(this);
		}

		public interface Handler extends NodeEvent.Handler {
			void onReplay(Replay event);
		}

		public interface Binding extends Handler {
			@Override
			default void onReplay(Replay event) {
				((Model) this).bindings().onNodeEvent(event);
			}
		}
	}

	@Directed(tag = "status")
	static class StatusView extends Model.All
			implements ModelTransform<SessionReplay.Status, StatusView> {
		List<KeyValue> keyValues = new ArrayList<>();

		@Property.Not
		Status status;

		@Override
		public StatusView apply(Status status) {
			KeyValue.stringValue("Event id", status.currentEventId)
					.addTo(keyValues);
			KeyValue.stringValue("State", status.state).addTo(keyValues);
			return this;
		}
	}

	@TypedProperties
	class ReplayArea extends Model.All implements Replay.Handler {
		PackageProperties._RomcomSessionDetailArea_ReplayArea.InstanceProperties
				properties() {
			return PackageProperties.romcomSessionDetailArea_replayArea
					.instance(this);
		}

		Heading heading = new Heading("Replay");

		@Directed.Transform(value = StatusView.class)
		SessionReplay.Status status = new SessionReplay.Status();

		Link replay = Link.button(Replay.class);

		@Override
		public void onReplay(Replay event) {
			NotificationObservable.of("Replaaaay!").publish();
			replay.setDisabled(true);
			SessionReplay replay = new SessionReplay(place.sequencePlace);
			bindings().fromTopic(replay.topicStatusChange)
					.accept(this::onStatusChange);
			replay.start();
		}

		void onStatusChange(SessionReplay.Status status) {
			properties().status().set(status);
			if (status.state == State.finished) {
				replay.setDisabled(false);
			}
		}
	}

	RomcomSessionDetailArea() {
		sequence = new SequenceComponentEditor("Events");
		sequence.sequence.component.elementLimit = 10;
		from(properties().place()).typed(RomcomSessionDetailPlace.class)
				.map(RomcomSessionDetailPlace::getSequencePlace)
				.accept(sequence::updateDefinition);
		on(SequenceComponentEditor.DefinitionChanged.class).value(
				() -> place.withUpdatedSequencePlace(sequence.sequencePlace))
				.accept(BasePlace::go);
	}

	@Override
	public void onNodeContext(NodeContext event) {
		entry = RomcomSessionProvider.get().getSession(place.path);
		sequenceMetadata = new SessionMetadata();
	}
}
