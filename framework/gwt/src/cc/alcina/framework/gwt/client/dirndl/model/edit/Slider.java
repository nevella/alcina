package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.stream.IntStream;

import com.google.gwt.dom.client.DomRect;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.Draggable;
import cc.alcina.framework.gwt.client.dirndl.event.Draggable.Dragged;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ReflectedEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ReflectedEvents.ZoomIn;
import cc.alcina.framework.gwt.client.dirndl.event.ReflectedEvents.ZoomOut;
import cc.alcina.framework.gwt.client.dirndl.layout.HandlesModelChange;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * <p>
 * This class models an editable double field (values between 0 and maxValue),
 * rendering as a Slider element.
 */
@TypedProperties
@TypeSerialization(reflectiveSerializable = false)
public class Slider extends Model.Fields implements HandlesModelChange,
		Draggable.Dragged.Handler, DomEvents.Click.Handler,
		ReflectedEvents.ZoomIn.Handler, ReflectedEvents.ZoomOut.Handler {
	public static class To implements ModelTransform<Double, Slider> {
		@Override
		public Slider apply(Double t) {
			if (t == null) {
				return null;
			}
			Slider result = new Slider();
			result.value = t;
			return result;
		}
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface Parameters {
		int width();

		int tickCount();
	}

	@TypedProperties
	class Knob extends Model.Fields implements Draggable {
		PackageProperties._Slider_Knob.InstanceProperties properties() {
			return PackageProperties.slider_knob.instance(this);
		}

		@Binding(type = Type.STYLE_ATTRIBUTE, transform = Binding.UnitPx.class)
		int left;

		Draggable.Support support;

		@Override
		public Support getDraggableSupport() {
			return support;
		}

		public Knob() {
			support = new Support(this);
		}

		@Override
		public void onNodeContext(NodeContext event) {
			from(Slider.this.properties().value()).map(v -> (int) (v * width))
					.to(properties().left()).oneWay();
		}
	}

	class Tick extends Model.Fields {
		@Binding(type = Type.STYLE_ATTRIBUTE, transform = Binding.UnitPx.class)
		int left;

		Tick(int index) {
			this.left = (width * index) / tickCount;
		}
	}

	public double value;

	int tickCount;

	@Binding(type = Type.STYLE_ATTRIBUTE, transform = Binding.UnitPx.class)
	int width;

	@Directed
	Object line = new Object();

	@Directed
	List<Tick> ticks;

	@Directed
	Knob knob = new Knob();

	@Override
	public void onNodeContext(NodeContext event) {
		Parameters parameters = event.getContext().node
				.annotation(Parameters.class);
		if (parameters != null) {
			this.width = parameters.width();
			this.tickCount = parameters.tickCount();
		}
		ticks = IntStream.rangeClosed(0, parameters.tickCount())
				.mapToObj(Tick::new).toList();
	}

	@Override
	public boolean handlesModelChange(PropertyChangeEvent evt) {
		properties().value().set((Double) evt.getNewValue());
		return true;
	}

	PackageProperties._Slider.InstanceProperties properties() {
		return PackageProperties.slider.instance(this);
	}

	@Override
	public void onDragged(Dragged event) {
	}

	@Override
	public void onClick(Click event) {
		NativeEvent nativeEvent = event.getContext()
				.getOriginatingNativeEvent();
		Element elem = provideElement();
		DomRect elemRect = elem.getBoundingClientRect();
		double offsetX = nativeEvent.getClientX() - elemRect.left;
		double newValue = offsetX / width;
		newValue = Math.max(0.0, newValue);
		newValue = Math.min(1.0, newValue);
		properties().value().set(newValue);
		event.reemitAs(this, ModelEvents.Change.class, value);
	}

	@Override
	public void onZoomOut(ZoomOut event) {
		deltaZoom(event, 1);
	}

	@Override
	public void onZoomIn(ZoomIn event) {
		deltaZoom(event, -1);
	}

	void deltaZoom(ModelEvent event, int i) {
		double next = value + ((double) i) * 0.1;
		next = Math.clamp(next, 0.0, 1.0);
		properties().value().set(next);
		event.reemitAs(this, ModelEvents.Change.class, value);
	}
}
