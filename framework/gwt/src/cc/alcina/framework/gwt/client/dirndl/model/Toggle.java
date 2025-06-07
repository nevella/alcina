package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

@TypeSerialization(reflectiveSerializable = false, flatSerializable = false)
@TypedProperties
public class Toggle extends Model.Fields implements ModelEvents.Toggle.Handler {
	public static transient PackageProperties._Toggle properties = PackageProperties.toggle;

	@Directed(reemits = { DomEvents.Click.class, ModelEvents.Toggle.class })
	public Object displayed;

	final List<Object> values;

	public int displayedIndex = 0;

	public Toggle(List<Object> values) {
		this.values = values;
		// this could also have been implemented by adding code to
		// setDisplayedIndex - but note that we get init for free this way
		bindings().from(this).on(properties.displayedIndex).typed(Integer.class)
				.accept(this::onDisplayedIndexSet);
	}

	public Object getActive() {
		int activeIndex = (displayedIndex + 1) % 2;
		return values.get(activeIndex);
	}

	void onDisplayedIndexSet(int newIndex) {
		properties.displayed.set(this, values.get(newIndex));
	}

	@Override
	public void onToggle(ModelEvents.Toggle event) {
		if (event.checkReemitted(this)) {
			return;
		}
		WidgetUtils.squelchCurrentEvent();
		int nextIndex = (displayedIndex + 1) % 2;
		properties.displayedIndex.set(this, nextIndex);
		event.reemit();
	}
}
