package cc.alcina.framework.gwt.client.dirndl.overlay;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.HasTag;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayEvents.RefreshPositioning;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPositions.ContainerOptions;

/**
 * <p>
 * Essentially non-functional (as a DOM event receiver) if the overlay is
 * non-modal, a glass (event interceptor) if it is modal
 * <p>
 * Note - this *must not* receive model events, since model events will be
 * routed to the Overlay.logicalParent (if any), not this
 *
 *
 *
 */
@Directed
public class OverlayContainer extends Model implements HasTag,
		Model.RerouteBubbledEvents, OverlayEvents.PositionedDescendants.Emitter,
		OverlayEvents.RefreshPositioning.Handler {
	private final Overlay contents;

	private final ContainerOptions containerOptions;

	private final boolean modal;

	private boolean visible = false;
	/*
	 * Used by the ViewportRelative.Transform
	 */

	private String className;

	@Binding(type = Type.CLASS_PROPERTY)
	public String getClassName() {
		return className;
	}

	OverlayContainer(Overlay contents, ContainerOptions containerOptions) {
		this.contents = contents;
		List<String> classes = new ArrayList<>();
		classes.add("overlay-container");
		if (containerOptions.position.viewportRelative != null) {
			classes.add("viewport-"
					+ Ax.cssify(containerOptions.position.viewportRelative));
		}
		if (Ax.notBlank(contents.getCssClass())) {
			classes.add(contents.getCssClass());
		}
		className = classes.stream().collect(Collectors.joining(" "));
		this.containerOptions = containerOptions;
		this.modal = containerOptions.modal;
		bindings()
				.addRegistration(() -> Window.addResizeHandler(this::onResize));
	}

	@Override
	public Model rerouteBubbledEventsTo() {
		if (contents.attributes.logicalParent == null) {
			return null;
		}
		if (contents.attributes.logicalParent.provideNode() != null) {
			return contents.attributes.logicalParent;
		}
		return contents.attributes.secondaryLogicalEventReroute;
	}

	@Directed
	public Model getContents() {
		return this.contents;
	}

	@Binding(
		to = "visibility",
		transform = Binding.VisibilityVisibleHidden.class,
		type = Type.STYLE_ATTRIBUTE)
	public boolean isVisible() {
		return this.visible;
	}

	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			Scheduler.get().scheduleDeferred(this::position);
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
		set("visible", this.visible, visible, () -> this.visible = visible);
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
		if (provideIsUnbound()) {
			return;
		}
		containerOptions.position.withToElement(provideElement()).apply();
		emitEvent(Overlay.Positioned.class, this);
		emitEvent(OverlayEvents.PositionedDescendants.class, this);
		Scheduler.get().scheduleFinally(() -> setVisible(true));
	}

	@Override
	public void onRefreshPositioning(RefreshPositioning event) {
		if (containerOptions.position.rectSourceElement == null) {
			return;
		}
		Scheduler.get().scheduleDeferred(() -> {
			if (containerOptions.position.rectSourceElement == null
					|| !containerOptions.position.rectSourceElement
							.isAttached()) {
				return;
			}
			containerOptions.position.fromRect = containerOptions.position.rectSourceElement
					.getBoundingClientRect();
			this.position();
		});
	}
}