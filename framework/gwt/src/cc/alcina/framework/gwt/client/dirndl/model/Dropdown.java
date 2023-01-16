package cc.alcina.framework.gwt.client.dirndl.model;

import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.ClickEvent;

import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.InferredDomEvents;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.DropdownEvents.DropdownButtonClicked;
import cc.alcina.framework.gwt.client.dirndl.model.DropdownEvents.InsideDropdownClicked;
import cc.alcina.framework.gwt.client.dirndl.model.DropdownEvents.OutsideDropdownClicked;

@Directed(
	bindings = @Binding(from = "open", type = Type.CSS_CLASS),
	receives = { DropdownEvents.DropdownButtonClicked.class,
			DropdownEvents.OutsideDropdownClicked.class,
			DropdownEvents.InsideDropdownClicked.class })
public class Dropdown<D extends Model> extends Model
		implements DropdownButtonClicked.Handler,
		OutsideDropdownClicked.Handler, InsideDropdownClicked.Handler {
	private boolean open;

	private Model button;

	private D dropdown;

	private D visibleDropdown;

	public Dropdown(Model button, D dropdown) {
		this.button = button;
		this.dropdown = dropdown;
	}

	@Directed(
		receives = DomEvents.Click.class,
		reemits = DropdownEvents.DropdownButtonClicked.class)
	public Model getButton() {
		return this.button;
	}

	public D getDropdown() {
		return this.dropdown;
	}

	@Directed(
		receives = { InferredDomEvents.ClickOutside.class,
				DomEvents.Click.class },
		reemits = { DropdownEvents.OutsideDropdownClicked.class,
				DropdownEvents.InsideDropdownClicked.class })
	public D getVisibleDropdown() {
		return this.visibleDropdown;
	}

	public boolean isOpen() {
		return this.open;
	}

	@Override
	public void onDropdownButtonClicked(DropdownButtonClicked event) {
		setOpen(!isOpen());
	}

	@Override
	public void onInsideDropdownClicked(InsideDropdownClicked event) {
		ClickEvent click = (ClickEvent) event
				.getContext().previous.previous.gwtEvent;
		Element element = Element.as(click.getNativeEvent().getEventTarget());
		if (!element.getTagName().equalsIgnoreCase("a")
				|| element.getAttribute("href").isEmpty()) {
			return;
		}
		setOpen(false);
	}

	@Override
	public void onOutsideDropdownClicked(OutsideDropdownClicked event) {
		setOpen(false);
	}

	public void setButton(Model button) {
		this.button = button;
	}

	public void setDropdown(D dropdown) {
		this.dropdown = dropdown;
		if (open) {
			setVisibleDropdown(dropdown);
		}
	}

	public void setOpen(boolean open) {
		setVisibleDropdown(open ? dropdown : null);
		boolean old_open = this.open;
		this.open = open;
		propertyChangeSupport().firePropertyChange("open", old_open, open);
	}

	public void setVisibleDropdown(D visibleDropdown) {
		D old_visibleDropdown = this.visibleDropdown;
		this.visibleDropdown = visibleDropdown;
		propertyChangeSupport().firePropertyChange("visibleDropdown",
				old_visibleDropdown, visibleDropdown);
	}
}