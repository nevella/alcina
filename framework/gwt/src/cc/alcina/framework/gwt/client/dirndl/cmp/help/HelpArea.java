package cc.alcina.framework.gwt.client.dirndl.cmp.help;

import com.google.gwt.activity.shared.PlaceUpdateable;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.activity.RootArea.ChannelOverlayPosition;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay.Attributes;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.ViewportRelative;

@TypedProperties
class HelpArea extends Model.Fields {
	@Directed
	public LeafModel.TagMarkup markup;

	HelpArea() {
		markup = new LeafModel.TagMarkup("full",
				HelpContentProvider.get().getHelpMarkup());
	}

	/**
	 * Link the containing area to the activity bus(es)
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	@Registration({ DirectedActivity.class, HelpPlace.class })
	static class ActivityRoute extends DirectedActivity
			// register in spite of non-public access
			implements Registration.AllSubtypes, PlaceUpdateable {
		@Directed
		HelpArea area;

		@Override
		public void onBeforeRender(BeforeRender event) {
			area = new HelpArea();
			super.onBeforeRender(event);
		}

		@Override
		public boolean canUpdate(PlaceUpdateable otherActivity) {
			/*
			 * All place updates are handled by the ToldArea
			 */
			return true;
		}
	}

	@Registration({ ChannelOverlayPosition.class, HelpPlace.class })
	static class ChannelOverlayPositionImpl extends ChannelOverlayPosition
			// register in spite of non-public access
			implements Registration.AllSubtypes {
		@Override
		public void position(Attributes builder) {
			builder.positionViewportRelative(ViewportRelative.BOTTOM_RIGHT);
		}
	}
}
