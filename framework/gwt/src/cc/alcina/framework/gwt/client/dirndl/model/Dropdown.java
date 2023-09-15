package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.model.DropdownEvents.DropdownButtonClicked;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay.Builder;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;

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
 * @param <D>
 */
@Directed(bindings = @Binding(from = "open", type = Type.CSS_CLASS))
public class Dropdown extends Model
		implements DropdownButtonClicked.Handler, ModelEvents.Closed.Handler {
	private boolean open;

	private Model button;

	private Model dropdown;

	private Overlay overlay;

	private OverlayPosition.Position xalign = Position.CENTER;

	private List<Model> dropdownStack = new ArrayList<>();

	private transient Supplier<Model> dropdownSupplier;

	private Supplier<List<Class<? extends Model>>> logicalAncestorsSupplier;

	private Model logicalParent;

	public Dropdown(Model button, Model dropdown) {
		this.button = button;
		setDropdown(dropdown);
	}

	public Dropdown(Model button, Supplier<Model> dropdownSupplier) {
		this.button = button;
		this.dropdownSupplier = dropdownSupplier;
		// the dropdown will be regenerated on show, this instnce acts as a
		// placeholder for the dropdown stack
		setDropdown(dropdownSupplier.get());
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

	public boolean isOpen() {
		return this.open;
	}

	@Override
	public void onClosed(Closed event) {
		// the popup closed, so change the corresponding state
		setOpen(false);
	}

	@Override
	public void onDropdownButtonClicked(DropdownButtonClicked event) {
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

	private void showDropdown(boolean show) {
		if (show) {
			if (dropdownSupplier != null && dropdownStack.size() == 1) {
				dropdownStack.clear();
				setDropdown(dropdownSupplier.get());
			}
			Builder builder = Overlay.builder();
			builder.dropdown(getXalign(),
					provideElement().getBoundingClientRect(), this, dropdown);
			if (dropdownStack.size() == 1 && logicalAncestorsSupplier != null) {
				builder.withLogicalAncestors(logicalAncestorsSupplier.get());
			}
			if (logicalParent == null) {
				logicalParent = this;
			}
			builder.withLogicalParent(logicalParent);
			overlay = builder.build();
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
}