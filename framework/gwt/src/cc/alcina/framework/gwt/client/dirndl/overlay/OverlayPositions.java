package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

@Registration.Singleton
public class OverlayPositions {
	public static OverlayPositions get() {
		return Registry.impl(OverlayPositions.class);
	}

	Map<Model, RenderedOverlay> openOverlays = AlcinaCollections.newUnqiueMap();

	void hide(Overlay model) {
		RenderedOverlay overlay = openOverlays.remove(model);
		if (overlay != null) {
			/*
			 * enqueue (don't mutate during event handling, since could trash
			 * the emitting node)
			 */
			Client.eventBus().queued().lambda(overlay::remove).dispatch();
		} else {
			throw new IllegalStateException(
					Ax.format("Removing previously removed overlay - %s",
							model.getContents()));
		}
	}

	void show(Overlay model, ContainerOptions containerOptions) {
		Preconditions.checkState(!openOverlays.containsKey(model));
		DirectedLayout layout = new DirectedLayout();
		Widget rendered = layout
				.render(new OverlayContainer(model, containerOptions));
		RenderedOverlay renderedOverlay = new RenderedOverlay(layout, rendered);
		openOverlays.put(model, renderedOverlay);
		RootPanel.get().add(rendered);
	}

	static class ContainerOptions {
		boolean modal;

		OverlayPosition position;

		boolean removeOnClickOutside;

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

	static class RenderedOverlay {
		DirectedLayout layout;

		Widget rendered;

		RenderedOverlay(DirectedLayout layout, Widget rendered) {
			this.layout = layout;
			this.rendered = rendered;
		}

		void remove() {
			rendered.removeFromParent();
			layout.remove();
		}
	}
}
