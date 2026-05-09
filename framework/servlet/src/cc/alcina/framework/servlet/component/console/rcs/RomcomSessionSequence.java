package cc.alcina.framework.servlet.component.console.rcs;

import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.service.InstanceOracle.Query;
import cc.alcina.framework.common.client.service.InstanceProvider;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequencePlace;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Link;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.servlet.logging.FlightEventRecorder;

public class RomcomSessionSequence
		extends Sequence.Abstract<RomcomSessionEntry> {
	@Override
	public SequenceSearchDefinition getDefaultSearchDefinition() {
		return new RomcomSessionSearchDefinition();
	}

	@TypedProperties
	static class RomcomSessionView extends Model.All
			implements Model.MultiNodeModel {
		Date start;

		Date end;

		String sessionId;

		int largestPacket;

		Link view;

		@Property.Not
		RomcomSessionEntry entry;

		RomcomSessionView(RomcomSessionEntry entry) {
			this.start = entry.start;
			this.end = entry.end;
			this.sessionId = entry.sessionId;
			this.largestPacket = entry.largestPacket;
			SequencePlace place = new SequencePlace();
			place.instanceQuery = FlightEventRecorder
					.createInstanceQuery(entry.path);
			String tokenString = place.toTokenString();
			String href = "/seq#" + tokenString;
			view = new Link().withText("View").withHref(href).withTargetBlank();
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
		return RomcomSessionView::new;
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
