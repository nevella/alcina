package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.function.Supplier;

import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Closed;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown.LabelArrow;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay;
import cc.alcina.framework.gwt.client.dirndl.overlay.Overlay.Positioned;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayContainer;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;

// emits ModelEvents.Change events
@Directed.Delegating
@Bean(PropertySource.FIELDS)
@TypeSerialization(reflectiveSerializable = false, flatSerializable = false)
public class ChoicesDropdown<T> extends Model.Value<T>
		implements ModelEvents.SelectionChanged.Handler,
		Overlay.Positioned.Handler, ModelEvents.Closed.Handler {
	Choices.Single<T> choices;

	@Directed
	Dropdown dropdown;

	T value;

	LabelArrow labelArrow;

	class ChoicesSupplier implements Supplier<Model> {
		@Override
		public Model get() {
			choices = new Choices.Single<>();
			choices.populateFromNodeContext(provideNode(), t -> t != value);
			choices.setSelectedValue(value);
			return choices;
		}
	}

	public static class To
			implements ModelTransform<Object, ChoicesDropdown<?>> {
		@Override
		public ChoicesDropdown<?> apply(Object t) {
			ChoicesDropdown<Object> dropdown = new ChoicesDropdown<>();
			dropdown.setValue(t);
			return dropdown;
		}
	}

	public ChoicesDropdown() {
		labelArrow = new Dropdown.LabelArrow(null);
		dropdown = new Dropdown(labelArrow, new ChoicesSupplier())
				.withLogicalParent(this).withXalign(Position.END);
		bindings().from(this).on("value").to(labelArrow).on("label").oneWay();
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		super.onBeforeRender(event);
	}

	@Override
	public T getValue() {
		return value;
	}

	@Override
	public void setValue(T value) {
		set("value", this.value, value, () -> this.value = value);
	}

	@Override
	public void onSelectionChanged(SelectionChanged event) {
		if (event.checkReemitted(this)) {
			return;
		}
		setValue(choices.getSelectedValue());
		dropdown.setOpen(false);
		event.reemit();
	}

	@Override
	public void onPositioned(Positioned event) {
		OverlayContainer ctr = event.getModel();
		DomRect rect = dropdown.provideElement().getBoundingClientRect();
		Element ctrElement = ctr.provideElement();
		DomRect overlayRect = ctrElement.getBoundingClientRect();
		ctrElement.getStyle().setLeft(rect.left, Unit.PX);
		ctrElement.getStyle().setPropertyPx("minWidth", (int) rect.width);
	}

	@Override
	public void onClosed(Closed event) {
		// squelch close events
	}
}