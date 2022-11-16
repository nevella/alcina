package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Registration.Singleton
public class OverlayPositions {
	public static OverlayPositions get() {
		return Registry.impl(OverlayPositions.class);
	}

	Map<Model, Widget> openOverlays = AlcinaCollections.newHashMap();

	public void show(Model model, boolean show) {
		if (show) {
			Preconditions.checkState(!openOverlays.containsKey(model));
			Widget rendered = new DirectedLayout()
					.render(new OverlayContainer(model));
			openOverlays.put(model, rendered);
			RootPanel.get().add(rendered);
		} else {
			openOverlays.get(model).removeFromParent();
		}
	}

	@Directed
	public static class OverlayContainer extends Model {
		private final Model contents;

		public OverlayContainer(Model contents) {
			this.contents = contents;
		}

		@Directed
		public Model getContents() {
			return this.contents;
		}
	}
}
