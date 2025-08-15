package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.HasSelectedValues;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * There are a *few* bits copied from Choices.Multiple, but it's more different
 * than not
 */
public class ChoicesEditorMultiple<T> extends ChoiceEditor<T>
		implements HasSelectedValues<T> {
	static PackageProperties._ChoicesEditorMultiple properties = PackageProperties.choicesEditorMultiple;

	public ChoicesEditorMultiple() {
		bindings().from(this).on(properties.selectedValues)
				.accept(this::updateAreaFromSelectedValues);
	}

	/*
	 * * Binds a collection property (in an editor) to a ChoicesEditorMultiple
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	public static class ListSuggestions<T> extends Model.Value<List<T>>
			implements ModelEvents.SelectionChanged.Handler {
		public static class To
				implements ModelTransform<List, ListSuggestions> {
			@Override
			public ListSuggestions apply(List t) {
				ListSuggestions suggest = new ListSuggestions();
				suggest.value = t;
				return suggest;
			}
		}

		@Directed
		public ChoicesEditorMultiple<T> editor;

		private List<T> value;

		public ListSuggestions() {
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			editor = new ChoicesEditorMultiple<>();
			// populate the delegate values from this node's AnnotationLocation
			editor.populateFromNodeContext(event.node, null);
			value = editor.getSelectedValues();
			super.onBeforeRender(event);
		}

		@Override
		public List<T> getValue() {
			return value;
		}

		@Override
		public void setValue(List<T> value) {
			set("value", this.value, value, () -> this.value = value);
			editor.setSelectedValues(value);
		}

		@Override
		public void onSelectionChanged(ModelEvents.SelectionChanged event) {
			setValue(editor.getSelectedValues());
		}
	}

	public List<T> getSelectedValues() {
		return selectedValues;
	}

	List<T> selectedValues;

	/*
	 * As per Choices.Multiple
	 */
	protected void emitChangeModelEvents(List<T> newValues) {
		emitEvent(ModelEvents.BeforeSelectionChangedDispatch.class, newValues);
		emitEvent(ModelEvents.BeforeSelectionChangedDispatchDescent.class,
				newValues);
		emitEvent(ModelEvents.SelectionChanged.class, newValues);
	}

	public void setSelectedValues(List<T> selectedValues) {
		if (!Objects.equals(selectedValues, this.selectedValues)) {
			set("selectedValues", this.selectedValues, selectedValues,
					() -> this.selectedValues = selectedValues);
			emitChangeModelEvents(selectedValues);
		}
	}

	@Override
	public Object provideSelectedValue() {
		return getSelectedValues();
	}

	@Override
	protected void onSelectedValues(List<T> selectedValues) {
		setSelectedValues(selectedValues);
	}
}
