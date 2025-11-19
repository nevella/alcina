package cc.alcina.framework.gwt.client.dirndl.model;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.OptionElement;
import com.google.gwt.dom.client.SelectElement;
import com.google.gwt.event.dom.client.MouseDownEvent;

import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.Ref;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.behaviour.IndexedSelection;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation;
import cc.alcina.framework.gwt.client.dirndl.behaviour.KeyboardNavigation.Navigation;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.Click;
import cc.alcina.framework.gwt.client.dirndl.event.DomEvents.MouseDown;
import cc.alcina.framework.gwt.client.dirndl.event.Filterable;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Filter;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Selected;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;
import cc.alcina.framework.gwt.client.dirndl.layout.HandlesModelChange;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.EnumValues.EnumSupplier;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.SelectResolver.OptionNameTransformer;
import cc.alcina.framework.gwt.client.objecttree.search.packs.SearchUtils;

/**
 * <p>
 * A generalised selection UI base class
 * 
 * <p>
 * If the set of selectable values is small and can be determined by the client
 * at render time (such as en enum, or a set of app-context buttons), the
 * selectable values are modelled by {@link #values} - concrete subclasses are:
 * <ul>
 * <li>{@link Choices.Select},{@link Choices.MultipleSelect} - render the
 * selectable values as an HTML select *
 * <li>{@link Choices.Single},{@link Choices.Multiple} - render the selectable
 * values as a set of {@link Choice} elements
 * </ul>
 * <p>
 * Note that in this first case, {@link #values} models <i>selectable</i>
 * values, not selected - the selected set or singleton is provided by
 * getSelectedValue(s) - and changes are signallred by
 * {@link ModelEvents.SelectionChanged}
 * <p>
 * When the selectable model is large - often remote, use
 */
@Directed(tag = "choices")
/*
 * I'm not entirely happy with 'Choices' firing 'Selection' events (could it not
 * be "chosen" events - or revert to "Selections/Selection") - even though
 * 'selected choice' is valid english and common parlance.
 *
 * But 'selection' is very heavily used in the codebase (including
 * SelectionTraversal) - so 'Choices+Selection' are the names we have.
 *
 * Also - possibly the wrapping with Choice could be done reflectively/more
 * elegantly
 *
 * Note - big one - clicking on a choice fires a 'Selected' event, but
 * programattic change does not - both fire a 'SelectionChanged' event so that's
 * generally what you want to observe
 *
 * FIXME - dirndl 1x1e - Now that overlay events are routed to logical parents,
 * it may be possible to cleanup event handling in this class
 * 
 * Note (docs) the dispatch order - the two 'before' events provide a lot of scope for customised handling:
 * 
 @formatter:off

	firePropertyChange("selectedValue", oldValue, newValue);
	firePropertyChange("value", oldValue, newValue);
	emitEvent(ModelEvents.BeforeSelectionChangedDispatch.class,
			newValue);
	emitEvent(
			ModelEvents.BeforeSelectionChangedDispatchDescent.class,
			newValue);
	emitEvent(ModelEvents.SelectionChanged.class, newValue);
	selectionChanged.signal();

@formatter:on
 * 
 */
public abstract class Choices<T> extends Model implements
		ModelEvents.Selected.Handler, HasSelectedValue, ContextResolver.Has,
		ModelEvents.BeforeSelectionChangedDispatchDescent.Emitter,
		ModelEvents.Filter.Handler {
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface EnumValues {
		@Reflected
		public static class EnumSupplier
				implements Function<EnumValues, List<?>> {
			@Override
			public List<?> apply(EnumValues v) {
				List result = new ArrayList<>();
				if (v.withNull()) {
					result.add(null);
				}
				Arrays.stream(v.value().getEnumConstants())
						.forEach(result::add);
				return result;
			}
		}

		Class<? extends Enum> value();

		boolean withNull() default false;

		boolean repeatedSelections() default false;
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface Categoriser {
		Class<? extends Function<?, String>> value();
	}

	/**
	 * If this exists on a property, the {@link Choices} will allow repeated
	 * selections of a choice (if the UI permits)
	 */
	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface RepeatableChoices {
	}

	@ClientVisible
	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	public @interface Values {
		public interface ValueSupplier
				extends Supplier<List<?>>, Function<Values, List<?>> {
			public interface ContextSensitive extends ValueSupplier {
				@Override
				default List<?> get() {
					throw new UnsupportedOperationException("Use 'apply'");
				}
			}

			@Override
			default List<?> apply(Values t) {
				return get();
			}

			default List<?> apply(Node contextNode, Values t) {
				return apply(t);
			}
		}

		/**
		 * The values supplier
		 */
		Class<? extends ValueSupplier> value();
	}

	/*
	 * Fired by a child of a choice to indicate the choice has been selected
	 */
	public static class ChoiceSelected
			extends ModelEvent<Object, ChoiceSelected.Handler> {
		public interface Handler extends NodeEvent.Handler {
			void onChoiceSelected(ChoiceSelected event);
		}

		@Override
		public void dispatch(ChoiceSelected.Handler handler) {
			handler.onChoiceSelected(this);
		}
	}

	/*
	 * Fired by keyboard naviagation, allow choice renderers (custom) to handle
	 * selection. If they handle, they must set the Ref<Boolean> event
	 * model/payload to true
	 */
	public static class ChoiceSelectedDescent extends
			ModelEvent.DescendantEvent<Ref<Boolean>, ChoiceSelectedDescent.Handler, ChoiceSelectedDescent.Emitter> {
		public interface Handler extends NodeEvent.Handler {
			void onChoiceSelectedDescent(ChoiceSelectedDescent event);
		}

		public interface Emitter extends ModelEvent.Emitter {
		}

		@Override
		public void dispatch(ChoiceSelectedDescent.Handler handler) {
			handler.onChoiceSelectedDescent(this);
		}
	}

	/*
	 * It is possible to bind just to isSelected here (with a delegating
	 * renderer) - but the css for decoration of the choice becomes a lot
	 * simpler if there _is_ a choice > model structure, so Choice remains a
	 * container for the moment
	 *
	 * Note the preventDefault on mouseDown - which prevents (say) choices in a
	 * ReferenceDecorator moving document focus
	 */
	@Directed(
		emits = { ModelEvents.Selected.class, ChoiceSelectedDescent.class })
	public static class Choice<T> extends Model
			implements DomEvents.Click.Handler, DomEvents.MouseDown.Handler,
			ChoiceSelected.Handler, ChoiceSelectedDescent.Emitter, Filterable {
		private boolean selected;

		private boolean indexSelected;

		private final T value;

		boolean multipleSelect;

		boolean filtered;

		public Choice(T value) {
			this.value = value;
		}

		@Binding(type = Type.PROPERTY)
		public boolean isFiltered() {
			return filtered;
		}

		public void setFiltered(boolean filtered) {
			set("filtered", this.filtered, filtered,
					() -> this.filtered = filtered);
		}

		@Directed
		public T getValue() {
			return this.value;
		}

		@Binding(type = Type.PROPERTY)
		public boolean isIndexSelected() {
			return this.indexSelected;
		}

		@Binding(type = Type.PROPERTY, to = "_selected")
		public boolean isSelected() {
			return this.selected;
		}

		@Override
		public void onClick(Click event) {
			if (multipleSelect) {
				return;
			}
			event.reemitAs(this, ModelEvents.Selected.class, this);
		}

		@Override
		public void onMouseDown(MouseDown event) {
			NativeEvent nativeEvent = ((MouseDownEvent) event.getContext()
					.getGwtEvent()).getNativeEvent();
			nativeEvent.preventDefault();
		}

		public void setIndexSelected(boolean indexSelected) {
			set("indexSelected", this.indexSelected, indexSelected,
					() -> this.indexSelected = indexSelected);
		}

		public void setSelected(boolean selected) {
			set("selected", this.selected, selected,
					() -> this.selected = selected);
		}

		@Override
		public void onChoiceSelected(ChoiceSelected event) {
			event.reemitAs(this, ModelEvents.Selected.class, this);
		}

		@Override
		public String toString() {
			return CommonUtils.nullSafeToString(value);
		}

		@Override
		public boolean matchesFilter(String filterValue) {
			String text = provideIsBound() ? provideElement().getTextContent()
					: CommonUtils.nullSafeToString(value);
			boolean matched = Ax.isBlank(filterValue)
					|| SearchUtils.matches(filterValue, text);
			setFiltered(!matched);
			return matched;
		}
	}

	@Directed(tag = "select")
	@DirectedContextResolver(SelectResolver.class)
	@Bean(PropertySource.FIELDS)
	public static class MultipleSelect<T> extends Multiple<T>
			implements DomEvents.Change.Handler {
		public static class To
				implements ModelTransform<List, MultipleSelect<?>> {
			@Override
			public MultipleSelect<?> apply(List t) {
				MultipleSelect<List> select = new MultipleSelect<>();
				select.setSelectedValues(t);
				return select;
			}
		}

		@Binding(type = Type.PROPERTY)
		public final boolean multiple = true;

		@Override
		public void onChange(Change event) {
			DirectedLayout.Node node = provideNode()
					.provideMostSpecificNodeForModel();
			SelectElement selectElement = (SelectElement) node.getRendered()
					.asElement();
			List<OptionElement> options = selectElement.getOptions().stream()
					.collect(Collectors.toList());
			List<T> selectedValues = new ArrayList<>();
			List<Choice<T>> selectedChoices = new ArrayList<>();
			for (int idx = 0; idx < options.size(); idx++) {
				OptionElement optionElement = options.get(idx);
				if (optionElement.isSelected()) {
					Choice<T> choice = choices.get(idx);
					selectedChoices.add(choice);
					selectedValues.add(choice.getValue());
				}
			}
			setSelectedValues(selectedValues);
			event.reemitAs(this, ModelEvents.SelectionChanged.class,
					selectedChoices);
		}

		@Override
		public void setSelectedValues(List<T> values) {
			super.setSelectedValues(values);
			if (provideIsBound()) {
				DirectedLayout.Node node = provideNode()
						.provideMostSpecificNodeForModel();
				SelectElement selectElement = (SelectElement) node.getRendered()
						.asElement();
				List<OptionElement> options = selectElement.getOptions()
						.stream().collect(Collectors.toList());
				List<T> selectedValues = new ArrayList<>();
				for (int idx = 0; idx < options.size(); idx++) {
					OptionElement optionElement = options.get(idx);
					Choice<T> choice = choices.get(idx);
					optionElement.setSelected(choice.isSelected());
				}
			}
		}
	}

	@Directed(emits = ModelEvents.SelectionChanged.class)
	@TypedProperties
	public static class Multiple<T> extends Choices<T>
			implements HasSelectedValues<T>, HandlesModelChange {
		public static class To<E>
				implements ModelTransform<List<E>, Multiple<E>> {
			@Override
			public Multiple<E> apply(List<E> t) {
				Multiple<E> result = new Multiple<>();
				result.setSelectedValues(t);
				return result;
			}
		}

		@Override
		public boolean handlesModelChange(PropertyChangeEvent evt) {
			setSelectedValues((List<T>) evt.getNewValue());
			return true;
		}

		List<T> unboundSelectedValues;

		public Multiple() {
		}

		public Multiple(List<T> values) {
			super(values);
		}

		public List<T> getSelectedValues() {
			return choices.stream().filter(Choice::isSelected)
					.map(Choice::getValue).collect(Collectors.toList());
		}

		@Override
		public void onSelected(Selected event) {
			if (event.checkReemitted(this)) {
				return;
			}
			Choices.Choice<T> choice = event == null ? null : event.getModel();
			T value = choice == null ? null : choice.getValue();
			List<T> updatedValues = choices.stream().filter(c -> {
				T choiceValue = c.getValue();
				// toggle inclusion
				if (c.isSelected()) {
					return choiceValue != value;
				} else {
					return choiceValue == value;
				}
			}).map(Choice::getValue).collect(Collectors.toList());
			setSelectedValues(updatedValues);
			event.reemit();
		}

		@Override
		public void onBind(Bind event) {
			unboundSelectedValues = null;
			super.onBind(event);
		}

		@Override
		public Object provideSelectedValue() {
			return getSelectedValues();
		}

		public void setSelectedValues(List<T> values) {
			if (provideIsUnbound()) {
				unboundSelectedValues = values;
			}
			List<T> oldValues = getSelectedValues();
			updateSelectedValuesInternal(values);
			List<T> newValues = getSelectedValues();
			if (!Objects.equals(oldValues, newValues)) {
				emitChangeModelEvents(newValues);
				propertyChangeSupport().firePropertyChange("selectedValues",
						oldValues, newValues);
			}
		}

		@Override
		public void setValues(List<T> values) {
			super.setValues(values);
			if (unboundSelectedValues != null) {
				updateSelectedValuesInternal(unboundSelectedValues);
			}
		}

		// do the actual update of selectedValues
		protected void updateSelectedValuesInternal(List<T> values) {
			Set valuesSet = new HashSet(values);
			choices.forEach(c -> c.setSelected(valuesSet.contains(c.value)));
		}

		protected void emitChangeModelEvents(List<T> newValues) {
			emitEvent(ModelEvents.BeforeSelectionChangedDispatch.class,
					newValues);
			emitEvent(ModelEvents.BeforeSelectionChangedDispatchDescent.class,
					newValues);
			emitEvent(ModelEvents.SelectionChanged.class, newValues);
		}
	}

	/*
	 * @OptionNameTransformer is used here to transform the choice.value to a
	 * String - which is used by the Option.Transform
	 */
	@Directed(tag = "select")
	public static class Select<T> extends Single<T>
			implements DomEvents.Change.Handler {
		public static class To implements ModelTransform<Object, Single<?>> {
			@Override
			public Select<?> apply(Object t) {
				Select<Object> select = new Select<>();
				select.setValue(t);
				return select;
			}
		}

		@Override
		@Property.Not
		public ContextResolver getContextResolver(AnnotationLocation location) {
			Class<? extends Function<?, String>> optionNameTransformerClass = location
					.optional(OptionNameTransformer.class)
					.map(ann -> ann.value()).orElse(null);
			return new SelectResolver(optionNameTransformerClass);
		}

		@Override
		public void onChange(Change event) {
			DirectedLayout.Node node = provideNode()
					.provideMostSpecificNodeForModel();
			SelectElement selectElement = (SelectElement) node.getRendered()
					.asElement();
			int index = selectElement.getSelectedIndex();
			Choice<T> choice = null;
			if (index >= 0) {
				choice = choices.get(index);
			}
			T value = choice != null ? choice.getValue() : null;
			setSelectedValue(value);
			event.reemitAs(this, ModelEvents.Selected.class, choice);
		}

		@Override
		public void setSelectedValue(T value) {
			super.setSelectedValue(value);
			if (provideIsBound()) {
				DirectedLayout.Node node = provideNode()
						.provideMostSpecificNodeForModel();
				SelectElement selectElement = (SelectElement) node.getRendered()
						.asElement();
				int index = getValues().indexOf(value);
				selectElement.setSelectedIndex(index);
			}
		}
	}

	/**
	 * Transforms a Choices model into an HTML Select. Note that the property of
	 * type Choices must have tag="select", since the resolver currently applies
	 * to child inputs, not the input on which it is declared
	 */
	public static class SelectResolver extends ContextResolver {
		@ClientVisible
		@Retention(RetentionPolicy.RUNTIME)
		@Documented
		@Target({ ElementType.METHOD, ElementType.FIELD })
		public @interface OptionNameTransformer {
			Class<? extends Function<?, String>> value();
		}

		public static class Option extends Choices.Choice<String> {
			public static class Transform extends
					AbstractContextSensitiveModelTransform<Choices.Choice, Option> {
				@Override
				public Option apply(Choice choice) {
					SelectResolver resolver = (SelectResolver) node
							.getResolver();
					Option option = new Option(
							resolver.transformOptionName(node, choice));
					option.setSelected(choice.isSelected());
					return option;
				}
			}

			public Option(String displayName) {
				super(displayName);
			}

			@Override
			@Directed.Exclude
			@Binding(type = Type.INNER_TEXT)
			public String getValue() {
				return super.getValue();
			}

			@Override
			@Binding(type = Type.PROPERTY)
			public boolean isSelected() {
				return super.isSelected();
			}

			@Override
			public void onClick(Click event) {
			}

			@Override
			public void onMouseDown(MouseDown event) {
			}
		}

		/*
		 * Style template
		 */
		@Bean
		public static class SelectTemplate {
			@Directed.TransformElements(Option.Transform.class)
			public List<Choices.Choice> getChoices() {
				return null;
			}
		}

		Function<Object, String> optionNameTransformer;

		public SelectResolver() {
		}

		protected SelectResolver(
				Class<? extends Function<?, String>> optionNameTransformerClass) {
			if (optionNameTransformerClass != null) {
				this.optionNameTransformer = (Function) Reflections
						.newInstance(optionNameTransformerClass);
			}
		}

		@Override
		protected Property resolveDirectedProperty0(Property property) {
			if (property.getDeclaringType() == Choices.class
					&& property.getName().equals("choices")) {
				return Reflections.at(SelectTemplate.class)
						.property(property.getName());
			} else {
				return super.resolveDirectedProperty0(property);
			}
		}

		/*
		 * Override to customize the default
		 */
		protected String transformOptionName(Node node, Choice choice) {
			return optionNameTransformer != null
					? optionNameTransformer.apply(choice.getValue())
					: HasDisplayName.displayName(choice.getValue(), "");
		}
	}

	@TypeSerialization(reflectiveSerializable = false)
	@Directed(
		emits = { ModelEvents.SelectionChanged.class,
				ModelEvents.Selected.class })
	public static class Single<T> extends Choices<T>
			implements KeyboardNavigation.Navigation.Handler, HasValue<T> {
		/**
		 * TODO - dirndl - add inner classes Enumeration and
		 * Enumeration.WithNull - to change
		 * 
		 * <pre>
		 * <code>
		 * 
		&#64;Directed.Transform(Choices.Single.To.class)
		&#64;Choices.EnumValues(InputOutputDisplayMode.class)
		
		to
		
		&#64;Directed.Transform(Choices.Single.To.Enumeration.class)
		 * </code>
		 * </pre>
		 */
		public static class To implements ModelTransform<Object, Single<?>> {
			@Override
			public Single<?> apply(Object t) {
				Single<Object> single = new Single<>();
				single.setValue(t);
				return single;
			}
		}

		/**
		 * A simple variant of {@link Single} for use in suggestor dropdowns
		 */
		@Directed.Delegating
		public static class Delegating<T> extends Single<T> {
			public Delegating(List<T> values) {
				super(values);
			}
		}

		class IndexedSelectionHostImpl implements IndexedSelection.Host {
			@Override
			public List getItems() {
				return choices;
			}
		}

		protected boolean deselectIfSelectedClicked = false;

		/*
		 * set to false to allow more complex selection logic
		 */
		protected boolean changeOnSelectionEvent = true;

		private T provisionalValue;

		protected T lastSelectedValue;

		/*
		 * Use ModelEvents by preference - this allows ex-hierarchy observation
		 * of changes if required (currently required for Overlay observation)
		 */
		private Topic<Void> selectionChanged = Topic.create();

		/*
		 * Use ModelEvents by preference - this allows ex-hierarchy observation
		 * of changes if required
		 */
		private Topic<Void> valueSelected = Topic.create();

		IndexedSelection indexedSelection;

		public Single() {
			this(new ArrayList<>());
		}

		public Single(List<T> values) {
			super(values);
			indexedSelection = new IndexedSelection(
					new IndexedSelectionHostImpl());
			indexedSelection.topicIndexChanged
					.add(this::onIndexedSelectionChange);
			updateIndexSelected(indexedSelection.getIndexSelected(), true);
		}

		public Single(T[] values) {
			this(List.of(values));
		}

		public T getProvisionalValue() {
			return this.provisionalValue;
		}

		public T getSelectedValue() {
			return choices.stream().filter(Choice::isSelected).findFirst()
					.map(Choice::getValue).orElse(null);
		}

		public boolean isChangeOnSelectionEvent() {
			return this.changeOnSelectionEvent;
		}

		public boolean isDeselectIfSelectedClicked() {
			return this.deselectIfSelectedClicked;
		}

		@Override
		public void onNavigation(Navigation event) {
			switch (event.getModel()) {
			case COMMIT:
				int indexSelected = indexedSelection.getIndexSelected();
				if (indexSelected != -1) {
					event.consume();
					// first, allow the choice to handle the keyboard selection
					// (custom)
					Choice<T> selectedChoice = choices.get(indexSelected);
					Ref<Boolean> handledMarker = Ref.of(false);
					event.reemitAs(selectedChoice, ChoiceSelectedDescent.class,
							handledMarker);
					if (!handledMarker.get()) {
						// not handled by the choice innards
						setSelectedValue(getValues().get(indexSelected));
					}
					return;
				}
				break;
			}
			indexedSelection.onNavigation(event);
		}

		@Override
		public void onSelected(Selected event) {
			if (event.checkReemitted(this)) {
				return;
			}
			Choices.Choice<T> choice = event == null ? null : event.getModel();
			T value = choice == null ? null : choice.getValue();
			if (deselectIfSelectedClicked && value == getSelectedValue()) {
				value = null;
			}
			provisionalValue = value;
			event.reemit();
			valueSelected.signal();
			if (changeOnSelectionEvent) {
				setSelectedValue(value);
			}
		}

		@Override
		public Object provideSelectedValue() {
			return getSelectedValue();
		}

		public void setChangeOnSelectionEvent(boolean changeOnSelectionEvent) {
			this.changeOnSelectionEvent = changeOnSelectionEvent;
		}

		public void setDeselectIfSelectedClicked(
				boolean deselectIfSelectedClicked) {
			this.deselectIfSelectedClicked = deselectIfSelectedClicked;
		}

		public void setSelectedValue(T value) {
			this.lastSelectedValue = value;
			T oldValue = getSelectedValue();
			choices.forEach(c -> c.setSelected(Objects.equals(c.value, value)));
			T newValue = getSelectedValue();
			if (provideIsBound() && !Objects.equals(oldValue, newValue)) {
				// FIXME - dirndl - probably can be replaced with
				// ValueChange.Bind (in general, bound Choices should be
				// constructed
				// via a Directed.Transform, not imperatively)
				firePropertyChange("selectedValue", oldValue, newValue);
				firePropertyChange("value", oldValue, newValue);
				emitEvent(ModelEvents.BeforeSelectionChangedDispatch.class,
						newValue);
				emitEvent(
						ModelEvents.BeforeSelectionChangedDispatchDescent.class,
						newValue);
				emitEvent(ModelEvents.SelectionChanged.class, newValue);
				selectionChanged.signal();
			}
		}

		@Override
		public void setValues(List<T> values) {
			super.setValues(values);
			choices.forEach(c -> c
					.setSelected(Objects.equals(c.value, lastSelectedValue)));
		}

		public ListenerReference subscribeSelectionChanged(Runnable runnable) {
			return selectionChanged.add(runnable);
		}

		public ListenerReference subscribeValueSelected(Runnable runnable) {
			return valueSelected.add(runnable);
		}

		public void toggle(Choice<T> choice) {
			if (getSelectedValue() == choice.getValue()) {
				setSelectedValue(null);
			} else {
				setSelectedValue(choice.getValue());
			}
		}

		@Override
		public T getValue() {
			return getSelectedValue();
		}

		@Override
		public void setValue(T t) {
			setSelectedValue(t);
		}

		void onIndexedSelectionChange(IndexedSelection.Change change) {
			updateIndexSelected(change.oldIndexSelected, false);
			updateIndexSelected(change.newIndexSelected, true);
		}

		void updateIndexSelected(int index, boolean indexSelected) {
			if (index >= 0 && index < choices.size()) {
				choices.get(index).setIndexSelected(indexSelected);
			}
		}
	}

	/*
	 * Dirndl docs - this is the basic pattern for a bound widget -- delegate,
	 * hold the value in the outer container and bind
	 */
	@Directed.Delegating
	@Registration({ Model.Value.class, FormModel.Editor.class, List.class })
	@Bean(PropertySource.FIELDS)
	public static class ListEditor<T> extends Model.Value<List<T>>
			implements ModelEvents.SelectionChanged.Handler {
		@Directed
		public MultipleSelect<T> select;

		private List<T> value;

		public ListEditor() {
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			select = new MultipleSelect<>();
			// populate the delegate values from this node's AnnotationLocation
			select.populateFromNodeContext(event.node, null);
			value = select.getSelectedValues();
			super.onBeforeRender(event);
		}

		@Override
		public List<T> getValue() {
			return value;
		}

		@Override
		public void setValue(List<T> value) {
			set("value", this.value, value, () -> this.value = value);
			select.setSelectedValues(value);
		}

		@Override
		public void onSelectionChanged(SelectionChanged event) {
			setValue(select.getSelectedValues());
		}
	}

	class ValueTransformerResolver
			extends ContextResolver.AnnotationCustomiser {
		ValueTransformerResolver() {
			resolveTransform(Choice.class, "value").with(valueTransformer);
		}
	}

	protected List<Choices.Choice<T>> choices;

	protected List<Model> elements;

	private List<T> values;

	public Class<? extends ModelTransform<T, ?>> valueTransformer;

	private boolean repeatableChoices;

	private Function<Object, String> categoriser;

	private CategoryEmitter categoryEmitter;

	public Choices() {
		this(new ArrayList<>());
	}

	public Choices(List<T> values) {
		setValues(values);
	}

	@Override
	@Property.Not
	public ContextResolver getContextResolver(AnnotationLocation location) {
		location.optional(ValueTransformer.class).ifPresent(
				ann -> valueTransformer = (Class<? extends ModelTransform<T, ?>>) ann
						.value());
		if (valueTransformer == null) {
			return null;
		} else {
			return new ValueTransformerResolver();
		}
	}

	public Optional<Choice> find(Predicate<Choice> predicate) {
		return (Optional<Choice>) (Optional<?>) choices.stream()
				.filter(predicate::test).findFirst();
	}

	/*
	 * choices + optional categories
	 */
	@Directed
	public List<Model> getElements() {
		return this.elements;
	}

	public List<Choices.Choice<T>> getChoices() {
		return this.choices;
	}

	public boolean isRepeatableChoices() {
		return repeatableChoices;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		populateFromNodeContext(event.node, null);
		super.onBeforeRender(event);
	}

	public List<T> getValues() {
		return this.values;
	}

	public void setChoices(List<Choices.Choice<T>> choices) {
		var old_choices = this.choices;
		this.choices = choices;
		propertyChangeSupport().firePropertyChange("choices", old_choices,
				choices);
	}

	class CategoryEmitter {
		List<Model> elements = new ArrayList<>();

		List<Category> categories = new ArrayList<>();

		public void accept(Choice choice) {
			if (categoriser != null) {
				String category = categoriser.apply(choice.getValue());
				Category categoryModel = Ax.last(categories);
				String lastCategoryText = categoryModel == null ? null
						: categoryModel.category;
				if (!Objects.equals(category, lastCategoryText)) {
					categoryModel = new Category(category);
					categories.add(categoryModel);
					elements.add(categoryModel);
				}
				if (categoryModel != null) {
					categoryModel.choices.add(choice);
				}
			}
			elements.add(choice);
		}
	}

	@TypedProperties
	class Category extends Model.Fields implements Filterable {
		PackageProperties._Choices_Category.InstanceProperties properties() {
			return PackageProperties.choices_category.instance(this);
		}

		Category(String category) {
			this.category = category;
		}

		List<Choice> choices = new ArrayList<>();

		@Binding(type = Type.PROPERTY)
		boolean filtered;

		@Binding(type = Type.INNER_TEXT)
		String category;

		@Override
		public boolean matchesFilter(String filterString) {
			properties().filtered()
					.set(choices.stream().allMatch(c -> c.filtered));
			return filtered;
		}
	}

	public void setValues(List<T> values) {
		this.values = values;
		List<Choice<T>> choices = new ArrayList<>();
		categoryEmitter = new CategoryEmitter();
		values.stream().forEach(value -> {
			Choice choice = new Choice(value);
			categoryEmitter.accept(choice);
			choices.add(choice);
		});
		setChoices(choices);
		setElements(categoryEmitter.elements);
	}

	public void setElements(List<Model> elements) {
		set("elements", this.elements, elements,
				() -> this.elements = elements);
	}

	@Override
	public void onFilter(Filter event) {
		choices.forEach(
				choice -> choice.matchesFilter(event.provideFilterValue()));
		if (categoryEmitter != null) {
			categoryEmitter.categories.forEach(category -> category
					.matchesFilter(event.provideFilterValue()));
		}
	}

	protected void populateFromNodeContext(Node node,
			Predicate<T> valueFilter) {
		if (node == null) {
			return;
		}
		node.optional(Categoriser.class)
				.ifPresent(ann -> this.categoriser = (Function) Reflections
						.newInstance(ann.value()));
		node.optional(Values.class).ifPresent(ann -> setValues(filter(
				(List<T>) Reflections.newInstance(ann.value()).apply(node, ann),
				valueFilter)));
		node.optional(EnumValues.class).ifPresent(ann -> setValues(
				filter((List<T>) new EnumSupplier().apply(ann), valueFilter)));
		repeatableChoices = node.has(RepeatableChoices.class);
	}

	List<T> filter(List<T> list, Predicate<T> valueFilter) {
		if (valueFilter == null) {
			return list;
		} else {
			return list.stream().filter(valueFilter)
					.collect(Collectors.toList());
		}
	}
}