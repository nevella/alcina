package cc.alcina.framework.gwt.client.dirndl.overlay;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPositions.ContainerOptions;

/**
 * <p>
 * Essentially unused if the overlay is non-modal, a glass (event interceptor)
 * if it is modal
 * <p>
 * Note - this *must not* receive model events, since model events will be
 * routed to the Overlay.logicalParent (if any), not this
 *
 *
 *
 */
@Directed(
	cssClass = "overlay-container",
	bindings = { @Binding(from = "viewportCentered", type = Type.CSS_CLASS),
			@Binding(
				from = "visible",
				to = "visibility",
				transform = Binding.VisibilityVisibleHidden.class,
				type = Type.STYLE_ATTRIBUTE) })
public class OverlayContainer extends Model implements HasTag {
	private final Overlay contents;

	private final ContainerOptions containerOptions;

	private final boolean modal;

	private boolean visible = false;

	OverlayContainer(Overlay contents, ContainerOptions containerOptions) {
		this.contents = contents;
		this.containerOptions = containerOptions;
		this.modal = containerOptions.modal;
		bindings()
				.addRegistration(() -> Window.addResizeHandler(this::onResize));
	}

	@Directed
	public Model getContents() {
		return this.contents;
	}

	public boolean getViewportCentered() {
		return containerOptions.position.viewportCentered;
	}

	public boolean isVisible() {
		return this.visible;
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			// FIXME - romcom
			if (GWT.isClient()) {
				Scheduler.get().scheduleFinally(this::position);
			} else {
				setVisible(true);
			}
		}
	}

	void onResize(ResizeEvent event) {
		if (!modal) {
			contents.close(event, false);
		} else {
			// FIXME - dirndl 1x1j - reposition in with an animation
			// gate
		}
	}

	/*
	 * Position the overlay according to containerOptions.overlayPosition,
	 * remove visibility:hidden
	 */
	void position() {
		containerOptions.position.toElement(provideElement()).apply();
		setVisible(true);
	}

	@Override
	public String provideTag() {
		// different tags (rather than css class) to support css
		// last-of-type
		return containerOptions.modal ? "overlay-container-modal"
				: "overlay-container";
	}

	public void setVisible(boolean visible) {
		boolean old_visible = this.visible;
		this.visible = visible;
		propertyChangeSupport().firePropertyChange("visible", old_visible,
				visible);
	}
}