package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.util.WidgetUtils;

@TypeSerialization(reflectiveSerializable = false, flatSerializable = false)
public class Toggle extends Model.Fields implements ModelEvents.Toggle.Handler {
	@Directed(reemits = { DomEvents.Click.class, ModelEvents.Toggle.class })
	public Object displayed;

	final List<Object> values;

	int displayedIndex = 0;

	public Object getActive() {
		int activeIndex = (displayedIndex + 1) % 2;
		return values.get(activeIndex);
	}

	public void setDisplayed(Object displayed) {
		set("displayed", this.displayed, displayed,
				() -> this.displayed = displayed);
	}

	public void setDisplayedIndex(int displayedIndex) {
		set(Property.displayedIndex, this.displayedIndex, displayedIndex,
				() -> this.displayedIndex = displayedIndex);
	}

	public Toggle(List<Object> values) {
		this.values = values;
		// this could also have been implemented by adding code to
		// setDisplayedIndex - but note that we get init for free this way
		bindings().from(this).on(Property.displayedIndex).typed(Integer.class)
				.accept(this::onDisplayedIndexSet);
	}

	void onDisplayedIndexSet(int newIndex) {
		setDisplayed(values.get(newIndex));
	}

	@Override
	public void onToggle(ModelEvents.Toggle event) {
		if (event.checkReemitted(this)) {
			return;
		}
		WidgetUtils.squelchCurrentEvent();
		int nextIndex = (displayedIndex + 1) % 2;
		setDisplayedIndex(nextIndex);
		event.reemit();
	}

	enum Property implements PropertyEnum {
		displayedIndex
	}
}
