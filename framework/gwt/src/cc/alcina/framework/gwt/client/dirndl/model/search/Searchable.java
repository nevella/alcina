package cc.alcina.framework.gwt.client.dirndl.model.search;

import java.util.function.Function;

import com.google.gwt.dom.client.EventBehavior;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.collections.FilterOperator;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.HasObject;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.reachability.Reflected;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.annotation.DirectedContextResolver;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.FocusEditor;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.BridgingValueRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TextTitle;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContextService;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;
import cc.alcina.framework.gwt.client.dirndl.model.edit.ChoicesEditorSingle;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorNode;
import cc.alcina.framework.gwt.client.dirndl.model.edit.FocusOnBindMarker;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/*
 * wip - ds - late - the choice edtior edit width should be determined by the
 * max popup width? possibly hardcoded *is* better. see
 * search-definition-editor.sass
 */
@TypedProperties
@DirectedContextResolver
class Searchable extends Model.Fields
		implements SuggestOracle.Suggestion.Noop, HasObject,
		DecoratorNode.AlllowsPartialSelection, ModelEvents.FocusEditor.Emitter,
		DecoratorNode.EditableDecoratorContents {
	/**
	 * Although these are implemented in .sass, this documents *why* they are so
	 */
	///
	/// transp border
	/// light grey
	/// hover: border
	/// hover: value displays white,light-border
	/// click: selects value
	///
	interface _Styles extends EventBehavior {
	}

	SearchCriterion searchCriterion;

	class Service implements ContextService {
		public SearchCriterion getSearchCriterion() {
			return searchCriterion;
		}
	}

	// for methodHandle
	SearchCriterion searchCriterion() {
		return searchCriterion;
	}

	@Directed
	String name;

	@Binding(type = Type.PROPERTY)
	String criterionClass;

	@Directed(tag = "operator")
	Dropdown operatorDropdown;

	@Directed
	ValueEditor valueEditor;

	@TypedProperties
	class RenderedOperator extends Model.Fields {
		@Directed.Transform(
			value = OperatorRenderer.class,
			transformsNull = true)
		StandardSearchOperator operator;

		PackageProperties._Searchable_RenderedOperator.InstanceProperties
				properties() {
			return PackageProperties.searchable_renderedOperator.instance(this);
		}

		@Override
		public void onNodeContext(NodeContext event) {
			/*
			 * wip - cookbook - note finaldispatch - otherwise cascading
			 * property changes cause render woes
			 */
			from(searchCriterion.searchCriterionProperties().operator())
					.withFinalDispatch().to(properties().operator()).oneWay();
		}
	}

	/*
	@formatter:off
	
	Notes on model/overlays when editing the operator

	Clicking on the dropdown opens the and overlay with contents OperatorSelector, which contains a 
	contenteditable [EditArea]. OnBind, that editor displays a choice-suggestor - which does not have an input
	(input comes from the EditArea), but does contain the results


	@formatter:on
	*/
	@TypedProperties
	class OperatorSelector extends Model.Fields
			implements ValueChange.Container {
		@Directed.Transform(
			// value = Choices.Select.To.class,
			value = ChoicesEditorSingle.SingleSuggestions.To.class,
			transformsNull = true)
		@FocusOnBindMarker
		@Choices.EnumValues(StandardSearchOperator.class)
		@ValueTransformer(ChoiceRenderer.To.class)
		StandardSearchOperator operator;

		PackageProperties._Searchable_OperatorSelector.InstanceProperties
				properties() {
			return PackageProperties.searchable_operatorSelector.instance(this);
		}

		@Override
		public void onNodeContext(NodeContext event) {
			from(searchCriterion.searchCriterionProperties().operator())
					.to(properties().operator()).bidi();
			from(properties().operator()).signal(() -> {
				if (provideIsBound()) {
					emitEvent(ModelEvents.Close.class);
				}
			});
		}
	}

	RenderedOperator renderedOperator;

	Searchable(SearchCriterion searchCriterion) {
		this.searchCriterion = searchCriterion;
		this.criterionClass = NestedName.get(searchCriterion);
		this.name = Ax.blankTo(searchCriterion.getDisplayName(), searchCriterion
				.getClass().getSimpleName().replace("Criterion", ""));
		renderedOperator = new RenderedOperator();
		operatorDropdown = new Dropdown(renderedOperator,
				() -> new OperatorSelector()).withLogicalAncestor(getClass())
						.withXalign(Position.START);
	}

	PackageProperties._Searchable.InstanceProperties properties() {
		return PackageProperties.searchable.instance(this);
	}

	String provideName() {
		return name;
	}

	class StringInputServiceImpl implements StringInput.Service {
		@Override
		public boolean isCommitOnEnter() {
			return true;
		}
	}

	public void onNodeContext(NodeContext event) {
		node.getResolver().registerService(StringInput.Service.class,
				new StringInputServiceImpl());
		node.getResolver().registerService(Service.class, new Service());
		properties().valueEditor().set(new ValueEditor());
		if (service(SearchDefinitionEditor.Service.class)
				.isInitialRenderComplete()) {
			exec(() -> emitEvent(FocusEditor.class)).dispatch();
		}
	}

	class ValueEditor extends Model.All {
		@Directed(renderer = BridgingValueRenderer.class)
		class ValueModelImpl implements ValueModel {
			@Override
			public Bindable getBindable() {
				return searchCriterion;
			}

			Field field;

			ValueModelImpl() {
				Property property = Reflections.at(searchCriterion)
						.property("value");
				field = BeanFields.query().forClass(searchCriterion.getClass())
						.forPropertyName("value").withEditable(true)
						.forMultipleWidgetContainer(false)
						.withValidationFeedbackProvider(
								new FormModel.ValidationFeedbackProvider())
						.withResolver(
								Searchable.this.provideNode().getResolver())
						.getField();
			}

			@Override
			public Field getField() {
				return field;
			}

			@Override
			public String getGroupName() {
				return null;
			}

			@Override
			public void onChildBindingCreated(
					com.totsp.gwittir.client.beans.Binding binding) {
			}
		}

		ValueModelImpl value;

		ValueEditor() {
			value = new ValueModelImpl();
		}

		@Override
		public void onNodeContext(NodeContext event) {
			node.getResolver().registerService(NodeEditorContextService.class,
					NodeEditorContextService.Editable.INSTANCE);
		}
	}

	@Override
	public Object provideObject() {
		return this;
	}

	@Override
	public String toString() {
		Object value = searchCriterion instanceof HasValue
				? ((HasValue) searchCriterion).getValue()
				: null;
		return Ax.format("%s : %s", name, Ax.toString(value));
	}

	@Property.Not
	@Override
	public boolean isEditableDecoratorContents() {
		return true;
	}

	static String operatorText(StandardSearchOperator operator,
			DirectedLayout.Node node) {
		if (operator == null) {
			return ":";
		}
		SearchCriterion searchCriterion = node.service(Service.class)
				.getSearchCriterion();
		if (Reflections.newInstance(searchCriterion.getClass())
				.getOperator() == operator) {
			return ":";
		}
		switch (operator) {
		case EQUALS:
			return "=";
		case CONTAINS:
		case AT_LEAST_ONE_OF:
			return "\u220B";
		case DOES_NOT_CONTAIN:
			return "\u220C";
		case ALL_OF:
			return "\u2287";
		case STARTS_WITH:
			return "\u2288";
		default:
			FilterOperator filterOperator = operator.toFilterOperator();
			if (filterOperator != null) {
				return filterOperator.operationText();
			}
			return Ax.friendly(operator);
		}
	}

	@Reflected
	static class OperatorRenderer extends
			AbstractContextSensitiveModelTransform<StandardSearchOperator, LeafModel.TextTitle> {
		String operatorText(StandardSearchOperator operator) {
			return Searchable.operatorText(operator, node);
		}

		@Override
		public TextTitle apply(StandardSearchOperator t) {
			TextTitle result = new TextTitle(operatorText(t),
					t == null ? "" : t.getName());
			return result;
		}
	}

	@Reflected
	static class ChoiceRenderer extends Model.All {
		static class To
				implements Function<StandardSearchOperator, ChoiceRenderer> {
			@Override
			public ChoiceRenderer apply(StandardSearchOperator operator) {
				return new ChoiceRenderer(operator);
			}
		}

		String operatorChar;

		String operatorName;

		@Property.Not
		StandardSearchOperator operator;

		ChoiceRenderer(StandardSearchOperator operator) {
			this.operator = operator;
		}

		@Override
		public void onNodeContext(NodeContext event) {
			this.operatorChar = Searchable.operatorText(operator, node);
			this.operatorName = operator.getName();
		}
	}
}