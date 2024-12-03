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
public class MultipleSuggestor<T> extends Multiple<T> {
	/**
	 * Renders a set of choices as a contenteditable - suitable for email
	 * 'to/cc/bcc' fields, etc
	 */
	@TypedProperties
	@Directed.Delegating
	public static class EditSuggestor extends Model.Fields {
		public static class To implements ModelTransform<List, EditSuggestor> {
			@Override
			public EditSuggestor apply(List t) {
				EditSuggestor suggest = new EditSuggestor();
				EditSuggestor.properties.choices.set(suggest, t);
				return suggest;
			}
		}

		static PackageProperties._MultipleSuggestor_EditSuggestor properties = PackageProperties.multipleSuggestor_editSuggestor;

		@Directed
		EditArea area;

		List<Choice<?>> choices;

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
	}

	/*
	 * * Binds a collection property (in an editor) to a MultipleSuggestor
	 */
	@Directed.Delegating
	@Bean(PropertySource.FIELDS)
	public static class ListSuggestor<T> extends Model.Value<List<T>>
			implements ModelEvents.SelectionChanged.Handler {
		@Directed
		public String captionish = "ish";

		@Directed
		public MultipleSuggestor<T> suggest;

		private List<T> value;

		public ListSuggestor() {
		}

		public static class To implements ModelTransform<List, ListSuggestor> {
			@Override
			public ListSuggestor apply(List t) {
				ListSuggestor suggest = new ListSuggestor();
				suggest.value = t;
				return suggest;
			}
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			suggest = new MultipleSuggestor<>();
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
}