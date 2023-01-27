package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Registration.Singleton
public class OverlayPositions {
	public static OverlayPositions get() {
		return Registry.impl(OverlayPositions.class);
	}

	Map<Model, Widget> openOverlays = AlcinaCollections.newUnqiueMap();

	void hide(Overlay model) {
		openOverlays.get(model).removeFromParent();
	}

	void show(Overlay model, ContainerOptions containerOptions) {
		Preconditions.checkState(!openOverlays.containsKey(model));
		Widget rendered = new DirectedLayout()
				.render(new OverlayContainer(model, containerOptions));
		openOverlays.put(model, rendered);
		RootPanel.get().add(rendered);
	}

	static class ContainerOptions {
		boolean modal;

		OverlayPosition position;

		boolean removeOnClickOutside;

		private String cssClass;

		public ContainerOptions withCssClass(String cssClass) {
			this.cssClass = cssClass;
			return this;
		}

		ContainerOptions withModal(boolean modal) {
			this.modal = modal;
			return this;
		}

		ContainerOptions withPosition(OverlayPosition position) {
			this.position = position;
			return this;
		}

		ContainerOptions
				withRemoveOnClickOutside(boolean removeOnClickOutside) {
			this.removeOnClickOutside = removeOnClickOutside;
			return this;
		}
	}
}
