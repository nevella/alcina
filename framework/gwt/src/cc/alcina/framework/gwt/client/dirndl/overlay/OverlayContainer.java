package cc.alcina.framework.gwt.client.dirndl.overlay;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Event.NativePreviewEvent;
import com.google.gwt.user.client.Window;

import cc.alcina.framework.gwt.client.browsermod.BrowserMod;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.LayoutEvents.Bind;
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
// FIXME - dirndl 1x1d - 1 - move preview etc handling to overlay (this is just
// the glass)
// - in fact, to an inferred dom event ('actionoutside') that is either dropped
// or causes close(), depending on modality
//
// TODO - intercept preview events a la GWT Dialog class (if modal). Or
// assign to overlay
//
// Note - event preview handling was essentially copied-and-truncated from
// GWT PopupPanel - it may be a little excessive
//
// Note - should observe source resize events and close if non-modal
//
//
public class OverlayContainer extends Model.WithNode implements HasTag {
	/**
	 *
	 */
	private final OverlayPositions overlayPositions;

	private final Overlay contents;

	private final ContainerOptions containerOptions;

	private HandlerRegistration nativePreviewHandlerRegistration;

	private final boolean modal;

	Boolean cachedIsMobile = null;

	private boolean visible = false;

	private HandlerRegistration resizeRegistration;

	OverlayContainer(OverlayPositions overlayPositions, Overlay contents,
			ContainerOptions containerOptions) {
		this.overlayPositions = overlayPositions;
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
			nativePreviewHandlerRegistration = Event
					.addNativePreviewHandler(this::previewNativeEvent);
			this.resizeRegistration = Window.addResizeHandler(this::onResize);
			Scheduler.get().scheduleFinally(this::position);
		} else {
			nativePreviewHandlerRegistration.removeHandler();
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

	/**
	 * Remove focus from an Element.
	 *
	 * @param elt
	 *            The Element on which <code>blur()</code> will be invoked
	 */
	private native void blur(Element elt) /*-{
    // Issue 2390: blurring the body causes IE to disappear to the background
    if (elt.blur && elt != $doc.body) {
      elt.blur();
    }
	}-*/;

	/**
	 * Does the event target this popup?
	 *
	 * @param event
	 *            the native event
	 * @return true if the event targets the popup
	 */
	private boolean eventTargetsPopup(NativeEvent event) {
		EventTarget target = event.getEventTarget();
		if (Element.is(target)) {
			return provideElement().isOrHasChild(Element.as(target));
		}
		return false;
	}

	// follows
	// com.google.gwt.user.client.ui.PopupPanel.previewNativeEvent(NativePreviewEvent)
	protected void previewNativeEvent(NativePreviewEvent event) {
		Event nativeEvent = Event.as(event.getNativeEvent());
		boolean keyEvent = false;
		switch (nativeEvent.getType()) {
		case BrowserEvents.KEYDOWN:
		case BrowserEvents.KEYPRESS:
		case BrowserEvents.KEYUP:
			EventTarget eventTarget2 = nativeEvent.getEventTarget();
			keyEvent = true;
			break;
		}
		// If the event has been canceled or consumed, ignore it
		if (event.isCanceled() || event.isConsumed()) {
			// We need to ensure that we cancel the event even if its been
			// consumed so
			// that popups lower on the stack do not auto hide
			if (modal) {
				event.cancel();
			}
			return;
		}
		if (event.isCanceled()) {
			return;
		}
		// If the event targets the popup or the partner, consume it
		boolean eventTargetsPopupOrPartner = eventTargetsPopup(nativeEvent);
		EventTarget eTarget = nativeEvent.getEventTarget();
		boolean eventTargetsScrollBar = Element.is(eTarget)
				&& Element.as(eTarget).getTagName().equalsIgnoreCase("html");
		if (cachedIsMobile == null) {
			cachedIsMobile = BrowserMod.isMobile();
		}
		boolean wasTouchMaybeDrag = cachedIsMobile && (BrowserEvents.TOUCHSTART
				.equals(nativeEvent.getType())
				|| BrowserEvents.TOUCHEND.equals(nativeEvent.getType())
				|| BrowserEvents.TOUCHMOVE.equals(nativeEvent.getType())
				|| BrowserEvents.GESTURECHANGE.equals(nativeEvent.getType())
				|| BrowserEvents.GESTUREEND.equals(nativeEvent.getType())
				|| BrowserEvents.GESTURESTART.equals(nativeEvent.getType())
				|| BrowserEvents.SCROLL.equals(nativeEvent.getType()));
		if (eventTargetsPopupOrPartner || eventTargetsScrollBar
				|| wasTouchMaybeDrag) {
			event.consume();
		}
		// Cancel the event if it doesn't target the modal popup. Note that
		// the
		// event can be both canceled and consumed.
		if (modal && !keyEvent) {
			event.cancel();
		}
		// Switch on the event type
		int type = nativeEvent.getTypeInt();
		switch (type) {
		case Event.ONMOUSEDOWN:
		case Event.ONTOUCHSTART:
			// Don't eat events if event capture is enabled, as this can
			// interfere with dialog dragging, for example.
			if (DOM.getCaptureElement() != null) {
				event.consume();
				return;
			}
			if (!eventTargetsPopupOrPartner && !modal) {
				this.overlayPositions.hide(contents);
				return;
			}
			break;
		case Event.ONMOUSEUP:
		case Event.ONMOUSEMOVE:
		case Event.ONCLICK:
		case Event.ONDBLCLICK: {
			// Don't eat events if event capture is enabled, as this can
			// interfere with dialog dragging, for example.
			if (DOM.getCaptureElement() != null) {
				event.consume();
				return;
			}
			break;
		}
		case Event.ONFOCUS: {
			EventTarget eventTarget = nativeEvent.getEventTarget();
			if (Element.is(eventTarget)) {
				Element target = eventTarget.cast();
				if (modal && !eventTargetsPopupOrPartner && (target != null)) {
					blur(target);
					event.cancel();
					return;
				}
			}
			break;
		}
		}
	}

	void onResize(ResizeEvent event) {
		if (!modal) {
			contents.close();
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