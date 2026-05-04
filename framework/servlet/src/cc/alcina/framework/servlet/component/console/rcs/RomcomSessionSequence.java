package cc.alcina.framework.servlet.component.console.rcs;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.service.InstanceOracle.Query;
import cc.alcina.framework.common.client.service.InstanceProvider;
import cc.alcina.framework.common.client.service.InstanceQuery;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.Sequence;
import cc.alcina.framework.gwt.client.dirndl.cmp.sequence.SequenceSearchDefinition;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

public class RomcomSessionSequence
		extends Sequence.Abstract<RomcomSessionEntry> {
	@Override
	public SequenceSearchDefinition getDefaultSearchDefinition() {
		return new RomcomSessionSearchDefinition();
	}

	@TypedProperties
	static class RomcomSessionView extends Model.Fields
			implements Model.MultiNodeModel {
		@Property.Not
		RomcomSessionEntry entry;

		RomcomSessionView(RomcomSessionEntry entry) {
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

	public static InstanceQuery
			createInstanceQuery(RomcomSessionSearchDefinition def) {
		return new InstanceQuery().withType(RomcomSessionSequence.class)
				.addParameters(new RomcomSessionSearchDefinition.Parameter()
						.withValue(def));
	}

	@InstanceProvider.Parameter(RomcomSessionSearchDefinition.Parameter.class)
	public static class InstanceProviderImpl
			implements InstanceProvider<RomcomSessionSequence> {
		@Override
		public RomcomSessionSequence provide(Query<RomcomSessionSequence> query)
				throws Exception {
			return new RomcomSessionSequence().withElements(List.of());
		}

		@Override
		public boolean isOneOff() {
			return true;
		}
	}
}
