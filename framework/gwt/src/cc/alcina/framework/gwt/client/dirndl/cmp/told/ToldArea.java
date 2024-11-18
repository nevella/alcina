package cc.alcina.framework.gwt.client.dirndl.cmp.told;

import com.google.gwt.activity.shared.PlaceUpdateable;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

class ToldArea extends Model.Fields {
	@Directed
	public String help = "Help!";

	/**
	 * Link the containing area to the activity bus(es)
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	@Registration({ DirectedActivity.class, ToldPlace.class })
	static class ActivityRoute extends DirectedActivity
			// register in spite of non-public access
			implements Registration.AllSubtypes, PlaceUpdateable {
		@Directed
		ToldArea area;

		@Override
		public void onBeforeRender(BeforeRender event) {
			area = new ToldArea();
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
}
