package cc.alcina.framework.gwt.client.dirndl.overlay;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPositions.ContainerOptions;

@Directed(
	cssClass = "overlay-container",
	bindings = { @Binding(from = "viewportCentered", type = Type.CSS_CLASS),
			@Binding(
				from = "visible",
				to = "visibility",
				transform = Binding.VisibilityVisibleHidden.class,
				type = Type.STYLE_ATTRIBUTE) })
public class OverlayContainer extends Model.WithNode implements HasTag {
	private final Overlay contents;

	private final ContainerOptions containerOptions;

	private final boolean modal;

	private boolean visible = false;

	private HandlerRegistration resizeRegistration;

	OverlayContainer(Overlay contents, ContainerOptions containerOptions) {
		this.contents = contents;
		this.containerOptions = containerOptions;
		this.modal = containerOptions.modal;
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
			this.resizeRegistration = Window.addResizeHandler(this::onResize);
			Scheduler.get().scheduleFinally(this::position);
		} else {
			resizeRegistration.removeHandler();
		}
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

	void onResize(ResizeEvent event) {
		if (!modal) {
			contents.close(false);
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
}