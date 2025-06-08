package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.Registration.EnvironmentSingleton;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.Al;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Rendered;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

@EnvironmentSingleton
@Registration.Singleton
public class OverlayPositions {
	public static OverlayPositions get() {
		return Registry.impl(OverlayPositions.class);
	}

	Map<Model, RenderedOverlay> openOverlays = AlcinaCollections.newUnqiueMap();

	void hide(Overlay model, boolean allowReentrant) {
		RenderedOverlay overlay = openOverlays.remove(model);
		if (overlay != null) {
			/*
			 * enqueue (don't mutate during event handling, since could trash
			 * the emitting node)
			 * 
			 * //FIX - romcom
			 */
			if (GWT.isClient()) {
				if (!Al.isRomcom()) {
					Element focussedDocumentElement = WidgetUtils
							.getFocussedDocumentElement();
					if (focussedDocumentElement != null && overlay.overlay
							.provideElement().provideIsAncestorOf(
									focussedDocumentElement, false)) {
						focussedDocumentElement.blur();
					}
				}
				Client.eventBus().queued().lambda(overlay::remove).dispatch();
			} else {
				// FIXME - romcom
				overlay.remove();
			}
		} else {
			if (allowReentrant) {
				// Overlay.remove will call back to here
			} else {
				throw new IllegalStateException(
						Ax.format("Removing previously removed overlay - %s",
								model.getContents()));
			}
		}
	}

	public void closeAll() {
		openOverlays.keySet().stream().collect(Collectors.toList())
				.forEach(m -> this.hide((Overlay) m, false));
	}

	void show(Overlay model, ContainerOptions containerOptions) {
		Preconditions.checkState(!openOverlays.containsKey(model));
		DirectedLayout layout = new DirectedLayout();
		ContextResolver resolver = ContextResolver.Default.get()
				.createResolver();
		if (model.attributes.logicalParent != null
				&& model.attributes.logicalParent.provideIsBound()) {
			resolver = model.attributes.logicalParent.provideNode()
					.getResolver();
		}
		Rendered rendered = layout
				.render(resolver, new OverlayContainer(model, containerOptions))
				.getRendered();
		RenderedOverlay renderedOverlay = new RenderedOverlay(layout, rendered,
				model);
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

		Overlay overlay;

		RenderedOverlay(DirectedLayout layout, Rendered rendered,
				Overlay overlay) {
			this.layout = layout;
			this.rendered = rendered;
			this.overlay = overlay;
		}

		void remove() {
			overlay.close(null, false);
			layout.remove();
		}
	}
}
