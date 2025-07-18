package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.shared.GwtEvent;

import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.KeyDown;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents.NativePreviewEventAsync;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.model.DropdownEvents.DropdownButtonClicked;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay.Attributes;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

/**
 * <p>
 * Presents the dropdown with an aboslute positioned overlay
 *
 * <p>
 * Has a few features: a dropdown supplier can be passed (to regenerate the
 * dropdown before display); also a replacement dropdown can be pushed if an
 * action causes another (but different) dropdown to be displayed at the same
 * logical application level - e.g. a dropdown shows a "color" action, the color
 * selector could be displayed as a pushed dropdown
 *
 *
 *
 */
@Directed
public class Dropdown extends Model
		implements DropdownButtonClicked.Handler, ModelEvents.Closed.Handler {
	private boolean open;

	protected Model button;

	protected Model dropdown;

	private Overlay overlay;

	private OverlayPosition.Position xalign = Position.CENTER;

	private List<Model> dropdownStack = new ArrayList<>();

	private transient Supplier<Model> dropdownSupplier;

	private Supplier<List<Class<? extends Model>>> logicalAncestorsSupplier;

	private Model logicalParent;

	private boolean display = true;

	@Binding(
		transform = Binding.DisplayBlankNone.class,
		type = Type.STYLE_ATTRIBUTE)
	public boolean isDisplay() {
		return display;
	}

	public void setDisplay(boolean display) {
		set("display", this.display, display, () -> this.display = display);
	}

	protected Dropdown() {
	}

	public Dropdown(Model button, Model dropdown) {
		this.button = button;
		setDropdown(dropdown);
	}

	public Dropdown(Model button, Supplier<Model> dropdownSupplier) {
		this.button = button;
		this.dropdownSupplier = dropdownSupplier;
		// the dropdown will be regenerated on show, this instance acts as a
		// placeholder for the dropdown stack
		setDropdown(dropdownSupplier.get());
	}

	static class LabelArrow extends Model.All
			implements DomEvents.Click.Handler, Binding.TabIndexZero,
			DomEvents.KeyDown.Handler {
		@Directed(
			tag = "label-area",
			reemits = { DomEvents.Click.class,
					DropdownEvents.ComboLabelSelected.class })
		Object label;

		public void setLabel(Object label) {
			set("label", this.label, label, () -> this.label = label);
		}

		@Directed(
			reemits = { DomEvents.Click.class,
					DropdownEvents.DropdownButtonClicked.class })
		Arrow arrow = new Arrow();

		public static class Arrow extends Model {
		}

		LabelArrow(Model label) {
			this.label = label;
		}

		@Override
		public void onClick(Click event) {
			// squelch any border clicks
		}

		@Override
		public void onKeyDown(KeyDown event) {
			switch (((KeyDownEvent) event.getContext().getGwtEvent())
					.getNativeKeyCode()) {
			case KeyCodes.KEY_ENTER:
			case KeyCodes.KEY_SPACE:
				event.reemitAs(this,
						DropdownEvents.DropdownButtonClicked.class);
				break;
			}
		}
	}

	@Directed(
		reemits = { DomEvents.Click.class,
				DropdownEvents.DropdownButtonClicked.class })
	public Model getButton() {
		return this.button;
	}

	public Model getDropdown() {
		return this.dropdown;
	}

	public Model getLogicalParent() {
		return this.logicalParent;
	}

	public OverlayPosition.Position getXalign() {
		return this.xalign;
	}

	@Binding(type = Type.CSS_CLASS)
	public boolean isOpen() {
		return this.open;
	}

	/*
	 * The overlay may be closed by a click on the dropdown (which is logically
	 * outside the overlay), interpreted as a MousedownOutside event on the
	 * overlay
	 * 
	 * If it was, don't then re-open on the click itself
	 */
	class ClosedOverlayData {
		long time;

		int screenX;

		int screenY;

		boolean mouseDown;

		ClosedOverlayData(Closed event) {
			GwtEvent originatingGwtEvent = event.getContext()
					.getOriginatingGwtEvent();
			if (originatingGwtEvent instanceof NativePreviewEventAsync) {
				NativePreviewEventAsync async = (NativePreviewEventAsync) originatingGwtEvent;
				NativeEvent nativeEvent = async.getNativeEvent();
				time = System.currentTimeMillis();
				if (nativeEvent.getType().equals("mousedown")) {
					this.mouseDown = true;
					screenX = nativeEvent.getScreenX();
					screenY = nativeEvent.getScreenY();
				}
			}
		}

		boolean isSuppress(DropdownButtonClicked event) {
			GwtEvent originatingGwtEvent = event.getContext()
					.getOriginatingGwtEvent();
			if (originatingGwtEvent instanceof ClickEvent) {
				ClickEvent click = (ClickEvent) originatingGwtEvent;
				return this.mouseDown
						&& TimeConstants.within(time,
								TimeConstants.ONE_SECOND_MS)
						&& click.getScreenX() == this.screenX
						&& click.getScreenY() == this.screenY;
			} else {
				return false;
			}
		}
	}

	ClosedOverlayData closedOverlayData;

	@Override
	public void onClosed(Closed event) {
		closedOverlayData = new ClosedOverlayData(event);
		// the popup closed, so change the corresponding state
		setOpen(false);
	}

	@Override
	public void onDropdownButtonClicked(DropdownButtonClicked event) {
		WidgetUtils.squelchCurrentEvent();
		if (!isOpen()) {
			if (closedOverlayData != null
					&& closedOverlayData.isSuppress(event)) {
				closedOverlayData = null;
				return;
			}
		}
		setOpen(!isOpen());
	}

	public void pushDropdown(Model model) {
		Preconditions.checkState(!open);
		// this must receive the closed event
		Preconditions.checkState(logicalParent == this);
		dropdownStack.add(model);
		dropdown = model;
	}

	public void setButton(Model button) {
		this.button = button;
	}

	public void setDropdown(Model dropdown) {
		this.dropdown = dropdown;
		if (dropdownStack.isEmpty()) {
			dropdownStack.add(dropdown);
		}
	}

	public void setOpen(boolean open) {
		boolean old_open = this.open;
		this.open = open;
		propertyChangeSupport().firePropertyChange("open", old_open, open);
		if (open && !old_open) {
			showDropdown(true);
		}
		if (old_open && !open) {
			showDropdown(false);
		}
	}

	public void toggleOpen() {
		setOpen(!open);
	}

	private void showDropdown(boolean show) {
		if (show) {
			if (dropdownSupplier != null && dropdownStack.size() == 1) {
				dropdownStack.clear();
				setDropdown(dropdownSupplier.get());
			}
			Attributes attributes = Overlay.attributes();
			attributes.dropdown(getXalign(),
					provideElement().getBoundingClientRect(), this, dropdown);
			if (dropdownStack.size() == 1 && logicalAncestorsSupplier != null) {
				attributes.withLogicalAncestors(logicalAncestorsSupplier.get());
			}
			if (logicalParent == null) {
				logicalParent = this;
			}
			attributes.withLogicalParent(logicalParent);
			overlay = attributes.create();
			overlay.open();
		} else {
			overlay.close(null, false);
			overlay = null;
			if (dropdownStack.size() > 1) {
				dropdownStack.remove(dropdownStack.size() - 1);
				dropdown = dropdownStack.get(dropdownStack.size() - 1);
			}
		}
	}

	public Dropdown
			withLogicalAncestor(Class<? extends Model> logicalAncestor) {
		return withLogicalAncestors(List.of(logicalAncestor));
	}

	public Dropdown withLogicalAncestors(
			List<Class<? extends Model>> logicalAncestors) {
		return withLogicalAncestorsSupplier(() -> logicalAncestors);
	}

	public Dropdown withLogicalAncestorsSupplier(
			Supplier<List<Class<? extends Model>>> logicalAncestorsSupplier) {
		this.logicalAncestorsSupplier = logicalAncestorsSupplier;
		return this;
	}

	/*
	 * Rare - when you want a dropdown overlay to stay visible , and the
	 * originating dropdown may disappear due to a rerender, set this somewhere
	 * higher in the node/model ancestor chain
	 */
	public Dropdown withLogicalParent(Model logicalParent) {
		this.logicalParent = logicalParent;
		return this;
	}

	public Dropdown withXalign(OverlayPosition.Position xalign) {
		this.xalign = xalign;
		return this;
	}
}