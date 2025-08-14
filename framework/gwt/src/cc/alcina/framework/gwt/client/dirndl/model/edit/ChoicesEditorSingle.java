package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;
import java.util.Objects;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.HasSelectedValues;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/*
 * There are a *few* bits copied from Choices.Single, but it's more different
 * than not
 * 
 * WIP - as per (say) Jira, the editor has a few wrinkles:
 * 
 * @formatter:off
 * - the cursor should always appear at the start of the area if when the editor is selected
 * - the existing choice should be display:none (from the editor) on input
 * - choice navigation behavior should be disabled (not exist)
 * - cursor css fix
 * - esc cancels (probably whole-editor behavior - revert to original values and fire a blur())
 * * @formatter:on
 */
public class ChoicesEditorSingle<T> extends ChoiceEditor<T>
		implements HasValue<T> {
	static PackageProperties._ChoicesEditorSingle properties = PackageProperties.choicesEditorSingle;

	public ChoicesEditorSingle() {
		bindings().from(this).on(properties.selectedValue)
				.accept(value -> this.updateAreaFromSelectedValue((T) value));
	}

	private T selectedValue;

	public T getSelectedValue() {
		return selectedValue;
	}

	public void setSelectedValue(T selectedValue) {
		if (!Objects.equals(selectedValue, this.selectedValue)) {
			set("selectedValue", this.selectedValue, selectedValue,
					() -> this.selectedValue = selectedValue);
			emitChangeModelEvents(selectedValue);
		}
	}

	/*
	 * * Binds a collection property (in an editor) to a MultipleSuggestor
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	public static class SingleSuggestions<T> extends Model.Value<T>
			implements ModelEvents.SelectionChanged.Handler {
		public static class To
				implements ModelTransform<Object, SingleSuggestions> {
			@Override
			public SingleSuggestions apply(Object t) {
				SingleSuggestions suggest = new SingleSuggestions();
				suggest.value = t;
				return suggest;
			}
		}

		@Directed
		public ChoicesEditorSingle<T> editor;

		private T value;

		public T getValue() {
			return value;
		}

		public void setValue(T value) {
			set("value", this.value, value, () -> this.value = value);
			editor.setSelectedValue(value);
		}

		public SingleSuggestions() {
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			editor = new ChoicesEditorSingle<>();
			// populate the delegate values from this node's AnnotationLocation
			editor.populateFromNodeContext(event.node, null);
			value = editor.getSelectedValue();
			super.onBeforeRender(event);
		}

		@Override
		public void onSelectionChanged(ModelEvents.SelectionChanged event) {
			setValue(editor.getSelectedValue());
		}
	}

	/*
	 * As per Choices.Multiple
	 */
	protected void emitChangeModelEvents(T newValue) {
		emitEvent(ModelEvents.BeforeSelectionChangedDispatch.class, newValue);
		emitEvent(ModelEvents.BeforeSelectionChangedDispatchDescent.class,
				newValue);
		emitEvent(ModelEvents.SelectionChanged.class, newValue);
	}

	@Override
	public Object provideSelectedValue() {
		return getSelectedValue();
	}

	@Override
	protected void onSelectedValues(List<T> selectedValues) {
		setSelectedValue(Ax.first(selectedValues));
	}

	@Override
	public T getValue() {
		return getSelectedValue();
	}

	@Override
	public void setValue(T t) {
		setSelectedValue(t);
	}
}
