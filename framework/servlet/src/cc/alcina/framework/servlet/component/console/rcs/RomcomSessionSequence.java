package cc.alcina.framework.servlet.component.console.rcs;

import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.service.InstanceOracle.Query;
import cc.alcina.framework.common.client.service.InstanceProvider;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.entity.util.Shell;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Mark;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Open;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NotificationObservable;
import cc.alcina.framework.servlet.logging.FlightEventRecorder;

public class RomcomSessionSequence
		extends Sequence.Abstract<RomcomSessionEntry> {
	@Override
	public SequenceSearchDefinition getDefaultSearchDefinition() {
		return new RomcomSessionSearchDefinition();
	}

	@Display.AllProperties
	@TypedProperties
	static class RomcomSessionView extends Model.Fields
			implements Model.MultiNodeModel {
		static PackageProperties._RomcomSessionSequence_RomcomSessionView properties = PackageProperties.romcomSessionSequence_romcomSessionView;

		Date start;

		Date end;

		String sessionId;

		int largestPacket;

		int slowestResponse;

		@Property.Not
		RomcomSessionEntry entry;

		RomcomSessionView(RomcomSessionEntry entry) {
			this.start = entry.start;
			this.end = entry.end;
			this.sessionId = entry.sessionId;
			this.largestPacket = entry.largestPacket;
			this.slowestResponse = entry.slowestResponse;
			this.entry = entry;
		}
	}

	@TypedProperties
	static class RomcomSessionDetail extends RomcomSessionView
			implements ModelEvents.Mark.Handler, ModelEvents.Open.Handler {
		List<Link> actions;

		RomcomSessionDetail(RomcomSessionEntry entry) {
			super(entry);
			SequencePlace place = new SequencePlace();
			place.instanceQuery = FlightEventRecorder
					.createInstanceQuery(entry.path);
			String tokenString = place.toTokenString();
			String href = "/seq#" + tokenString;
			RomcomSessionDetailPlace detailPlace = new RomcomSessionDetailPlace(
					entry.path);
			Link view = Link.of(detailPlace).withText("View");
			Link events = new Link().withText("Events").withHref(href)
					.withTargetBlank();
			actions = List.of(view, events, Link.of(ModelEvents.Mark.class),
					Link.of(ModelEvents.Open.class));
		}

		@Override
		public void onMark(Mark event) {
			entry.marked = true;
			entry.persist();
			NotificationObservable.of("Session %s marked", entry.sessionId)
					.publish();
		}

		@Override
		public void onOpen(Open event) {
			Shell.exec("open '%s'", entry.path);
		}
	}

	@Override
	public ModelTransform<RomcomSessionEntry, ? extends Model>
			getRowTransform() {
		return RomcomSessionView::new;
	}

	@Override
	public ModelTransform<RomcomSessionEntry, ? extends Model>
			getDetailTransform() {
		return RomcomSessionDetail::new;
	}

	public static InstanceQuery createInstanceQuery() {
		return new InstanceQuery().withType(RomcomSessionSequence.class);
	}

	public static class InstanceProviderImpl
			implements InstanceProvider<RomcomSessionSequence> {
		@Override
		public RomcomSessionSequence provide(Query<RomcomSessionSequence> query)
				throws Exception {
			List<RomcomSessionEntry> entries = RomcomSessionProvider.get()
					.getSessions().toList();
			return new RomcomSessionSequence().withElements(entries);
		}

		@Override
		public boolean isOneOff() {
			return true;
		}
	}
}
