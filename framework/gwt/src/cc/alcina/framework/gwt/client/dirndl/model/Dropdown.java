package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.ArrayList;
import java.util.List;

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
 * Presents the dropdown with an aboslute positioned overlay
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

	public Dropdown(Model button, Model dropdown) {
		this.button = button;
		setDropdown(dropdown);
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

	private void showDropdown(boolean show) {
		if (show) {
			Builder builder = Overlay.builder();
			overlay = builder
					.dropdown(getXalign(),
							provideElement().getBoundingClientRect(), dropdown)
					.withCloseHandler(evt -> setOpen(false)).build();
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