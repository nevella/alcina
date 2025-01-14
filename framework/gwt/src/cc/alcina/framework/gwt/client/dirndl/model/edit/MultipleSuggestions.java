package cc.alcina.framework.gwt.client.dirndl.model.edit;

import java.util.List;
import java.util.function.Predicate;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Multiple;
import cc.alcina.framework.gwt.client.dirndl.model.Model;

/**
 * Renders a set of choices as a contenteditable - suitable for email
 * 'to/cc/bcc' fields, etc
 */
@Bean(PropertySource.FIELDS)
public class MultipleSuggestions<T> extends Multiple<T> {
	@Override
	@Directed.Transform(EditSuggestor.To.class)
	public List<Choice<T>> getChoices() {
		return super.getChoices();
	}

	@Override
	protected void populateValuesFromNodeContext(Node node,
			Predicate<T> valueFilter) {
		// provides access to ListSuggestor
		super.populateValuesFromNodeContext(node, valueFilter);
	}

	/**
	 * Renders a set of choices as a contenteditable - suitable for email
	 * 'to/cc/bcc' fields, etc
	 */
	@TypedProperties
	@Directed.Delegating
	static class EditSuggestor extends Model.Fields {
		static PackageProperties._MultipleSuggestions_EditSuggestor properties = PackageProperties.multipleSuggestions_editSuggestor;

		EditArea area;

		EditSuggestor() {
			area = new EditArea();
			// area.provideFragmentModel().addModelled(type);
			bindings().from(this).on(properties.choices)
					.accept(this::areaContentsFromChoices);
		}
		// @Directed(tag = "mention")
		// @Bean(PropertySource.FIELDS)
		// static class ChoiceNode extends DecoratorNode<Choice> {
		// }

		void areaContentsFromChoices(List choices) {
		}

		List<Choice<?>> choices;

		public static class To implements ModelTransform<List, EditSuggestor> {
			@Override
			public EditSuggestor apply(List t) {
				EditSuggestor suggest = new EditSuggestor();
				EditSuggestor.properties.choices.set(suggest, t);
				return suggest;
			}
		}
	}

	/*
	 * * Binds a collection property (in an editor) to a MultipleSuggestions
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	public static class ListSuggestor<T> extends Model.Value<List<T>>
			implements ModelEvents.SelectionChanged.Handler {
		@Directed
		public MultipleSuggestions<T> suggest;

		private List<T> value;

		public ListSuggestor() {
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			suggest = new MultipleSuggestions<>();
			// populate the delegate values from this node's AnnotationLocation
			suggest.populateValuesFromNodeContext(event.node, null);
			value = suggest.getSelectedValues();
			super.onBeforeRender(event);
		}

		@Override
		public List<T> getValue() {
			return value;
		}

		@Override
		public void setValue(List<T> value) {
			set("value", this.value, value, () -> this.value = value);
			suggest.setSelectedValues(value);
		}

		@Override
		public void onSelectionChanged(SelectionChanged event) {
			setValue(suggest.getSelectedValues());
		}
	}
}