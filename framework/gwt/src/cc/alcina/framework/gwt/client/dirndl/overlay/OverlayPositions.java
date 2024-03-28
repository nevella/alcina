package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;
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
			if (GWT.isClient()) {
				Client.eventBus().queued().lambda(overlay::remove).dispatch();
			} else {
				// FIXME - romcom
				overlay.remove();
			}
		} else {
			throw new IllegalStateException(
					Ax.format("Removing previously removed overlay - %s",
							model.getContents()));
		}
	}

	public void closeAll() {
		openOverlays.keySet().stream().toList()
				.forEach(m -> this.hide((Overlay) m));
	}

	void show(Overlay model, ContainerOptions containerOptions) {
		Preconditions.checkState(!openOverlays.containsKey(model));
		DirectedLayout layout = new DirectedLayout();
		Rendered rendered = layout
				.render(new OverlayContainer(model, containerOptions))
				.getRendered();
		RenderedOverlay renderedOverlay = new RenderedOverlay(layout, rendered);
		openOverlays.put(model, renderedOverlay);
		rendered.appendToRoot();
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

		Rendered rendered;

		RenderedOverlay(DirectedLayout layout, Rendered rendered) {
			this.layout = layout;
			this.rendered = rendered;
		}

		void remove() {
			layout.remove();
		}
	}
}
