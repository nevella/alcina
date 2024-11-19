package cc.alcina.framework.servlet.component.featuretree;

import com.google.gwt.activity.shared.PlaceUpdateable;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Directed
class Page extends Model.All {
	Header header = new Header();

	Main main = new Main();

	static class Header extends Model.All {
		String name = FeatureTree.Ui.get().getMainCaption();
	}

	/**
	 * This activity hooks the Page up to the RootArea (the general routing
	 * contract)
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	@Registration({ DirectedActivity.class, FeaturePlace.class })
	static class ActivityRoute extends DirectedActivity
			// register in spite of non-public access
			implements Registration.AllSubtypes, PlaceUpdateable,
			ModelEvent.DelegatesDispatch {
		@Directed
		Page page;

		@Override
		public void onBeforeRender(BeforeRender event) {
			page = new Page();
			super.onBeforeRender(event);
		}

		@Override
		public boolean canUpdate(PlaceUpdateable otherActivity) {
			/*
			 * All place updates are handled by the Page
			 */
			return true;
		}

		@Override
		public Model provideDispatchDelegate() {
			return page;
		}
	}

	static class Main extends Model.All {
		FeatureTable featureTable;

		Properties properties;

		Documentation documentation = new Documentation();

		Main() {
			featureTable = new FeatureTable();
			properties = new Properties(featureTable.features);
		}
	}
}
