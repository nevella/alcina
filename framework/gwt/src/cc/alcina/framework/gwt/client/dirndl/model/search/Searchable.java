package cc.alcina.framework.gwt.client.dirndl.model.search;

import java.util.List;
import java.util.function.Function;

import com.google.gwt.dom.client.EventBehavior;
import com.google.gwt.user.client.ui.SuggestOracle;

import cc.alcina.framework.common.client.collections.FilterOperator;
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
import cc.alcina.framework.gwt.client.dirndl.layout.ContextService;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel;
import cc.alcina.framework.gwt.client.dirndl.layout.LeafModel.TextTitle;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform.AbstractContextSensitiveModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Choices.Values;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorNode;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;
import cc.alcina.framework.gwt.client.dirndl.overlay.OverlayPosition.Position;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

/*
 * wip - ds - late - the choice editor edit width should be determined by the
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
	Object operator;

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

	RenderedOperator renderedOperator;

	Searchable(SearchCriterion searchCriterion) {
		this.searchCriterion = searchCriterion;
		this.criterionClass = NestedName.get(searchCriterion);
		this.name = Ax.blankTo(searchCriterion.getDisplayName(), searchCriterion
				.getClass().getSimpleName().replace("Criterion", ""));
		renderedOperator = new RenderedOperator();
		List<StandardSearchOperator> applicableOperators = searchCriterion
				.getApplicableOperators();
		if (applicableOperators.size() == 1) {
			operator = ":";
		} else {
			operator = new Dropdown(renderedOperator,
					() -> new OperatorSelector(searchCriterion
							.searchCriterionProperties().operator()))
									.withLogicalAncestor(getClass())
									.withXalign(Position.START);
		}
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
		properties().valueEditor().set(new ValueEditor(
				searchCriterion.valueProperty(), node.getResolver()));
		if (service(SearchDefinitionEditor.Service.class)
				.isInitialRenderComplete()) {
			exec(() -> emitEvent(FocusEditor.class)).dispatch();
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
			DirectedLayout.Node node, boolean colonIfDefault) {
		if (operator == null) {
			return ":";
		}
		SearchCriterion searchCriterion = node.service(Service.class)
				.getSearchCriterion();
		if (Reflections.newInstance(searchCriterion.getClass())
				.getOperator() == operator && colonIfDefault) {
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
		/*
		 * both the same (superset/equals) - treating a string as an ordered set
		 * // of chars. yeah sure, so does contains...
		 */
		case ALL_OF:
		case STARTS_WITH:
			return "\u2287";
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
			return Searchable.operatorText(operator, node, true);
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
		@Reflected
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
			this.operatorChar = Searchable.operatorText(operator, node, false);
			this.operatorName = operator.getName();
		}
	}

	@Reflected
	static class AvailableOperators
			implements Choices.Values.ValueSupplier.ContextSensitive {
		@Override
		public List<?> apply(DirectedLayout.Node contextNode, Values t) {
			return contextNode.service(Service.class).getSearchCriterion()
					.getApplicableOperators();
		}
	}
}