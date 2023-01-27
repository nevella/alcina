package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
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
 * @author nick@alcina.cc
 *
 * @param <D>
 */
@Directed(
	bindings = @Binding(from = "open", type = Type.CSS_CLASS),
	receives = { DropdownEvents.DropdownButtonClicked.class })
public class Dropdown extends Model.WithNode
		implements DropdownButtonClicked.Handler {
	private boolean open;

	private Model button;

	private Model dropdown;

	private Overlay overlay;

	private OverlayPosition.Position xalign = Position.CENTER;

	private List<Model> dropdownStack = new ArrayList<>();

	private transient Supplier<Model> dropdownSupplier;

	private transient Supplier<String> dropdownCssClassSupplier;

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
		receives = DomEvents.Click.class,
		reemits = DropdownEvents.DropdownButtonClicked.class)
	public Model getButton() {
		return this.button;
	}

	public Model getDropdown() {
		return this.dropdown;
	}

	public OverlayPosition.Position getXalign() {
		return this.xalign;
	}

	public boolean isOpen() {
		return this.open;
	}

	@Override
	public void onDropdownButtonClicked(DropdownButtonClicked event) {
		setOpen(!isOpen());
	}

	public void pushDropdown(Model model) {
		Preconditions.checkState(!open);
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

	public void setXalign(OverlayPosition.Position xalign) {
		this.xalign = xalign;
	}

	public Dropdown withDropdownCssClass(String dropdownCssClass) {
		return withDropdownCssClassSupplier(() -> dropdownCssClass);
	}

	public Dropdown withDropdownCssClassSupplier(
			Supplier<String> dropdownCssClassSupplier) {
		this.dropdownCssClassSupplier = dropdownCssClassSupplier;
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
					provideElement().getBoundingClientRect(), dropdown)
					.withPeer(this).withCloseHandler(evt -> setOpen(false));
			if (dropdownStack.size() == 1 && dropdownCssClassSupplier != null) {
				builder.withCssClass(dropdownCssClassSupplier.get());
			}
			overlay = builder.build();
			overlay.open();
		} else {
			overlay.close(false);
			overlay = null;
			if (dropdownStack.size() > 1) {
				dropdownStack.remove(dropdownStack.size() - 1);
				dropdown = dropdownStack.get(dropdownStack.size() - 1);
			}
		}
	}
}