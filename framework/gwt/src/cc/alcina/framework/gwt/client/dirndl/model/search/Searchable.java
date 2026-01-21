package cc.alcina.framework.gwt.client.dirndl.model.search;

import com.google.gwt.dom.client.EventBehavior;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.domain.HasObject;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding;
import cc.alcina.framework.gwt.client.dirndl.annotation.Binding.Type;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.BridgingValueRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DelegatingContextResolver;
import cc.alcina.framework.gwt.client.dirndl.model.Choices;
import cc.alcina.framework.gwt.client.dirndl.model.Dropdown;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContext;
import cc.alcina.framework.gwt.client.dirndl.model.edit.DecoratorNode;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;
import cc.alcina.framework.gwt.client.objecttree.search.StandardSearchOperator;

@TypedProperties
class Searchable extends Model.Fields implements SuggestOracle.Suggestion.Noop,
		HasObject, DecoratorNode.AlllowsPartialSelection {
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

	// for methodHandle
	SearchCriterion searchCriterion() {
		return searchCriterion;
	}

	@Directed
	String name;

	@Binding(type = Type.PROPERTY)
	String criterionClass;

	@Directed
	Dropdown operatorDropdown;

	@Directed
	ValueEditor valueEditor;

	@TypedProperties
	class RenderedOperator extends Model.Fields {
		@Directed.Transform(
			value = StandardSearchOperator.SimpleRenderer.class,
			transformsNull = true)
		StandardSearchOperator operator;

		PackageProperties._Searchable_RenderedOperator.InstanceProperties
				properties() {
			return PackageProperties.searchable_renderedOperator.instance(this);
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			/*
			 * wip - cookbook - note finaldispatch - otherwise cascading
			 * property changes cause render woes
			 */
			from(searchCriterion.searchCriterionProperties().operator())
					.withFinalDispatch().to(properties().operator()).oneWay();
			super.onBeforeRender(event);
		}
	}

	@TypedProperties
	class OperatorSelector extends Model.Fields
			implements ValueChange.Container {
		@Directed.Transform(
			value = Choices.Select.To.class,
			transformsNull = true)
		@Choices.EnumValues(StandardSearchOperator.class)
		StandardSearchOperator operator;

		PackageProperties._Searchable_OperatorSelector.InstanceProperties
				properties() {
			return PackageProperties.searchable_operatorSelector.instance(this);
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			from(properties().operator())
					.to(searchCriterion.searchCriterionProperties().operator())
					.bidi();
			super.onBeforeRender(event);
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
				() -> new OperatorSelector()).withLogicalParent(this);
	}

	PackageProperties._Searchable.InstanceProperties properties() {
		return PackageProperties.searchable.instance(this);
	}

	String provideName() {
		return name;
	}

	/*
	 * wip - ds - move to onNodeContext
	 */
	@Override
	public void onBind(Bind event) {
		super.onBind(event);
		if (event.isBound()) {
			exec(() -> properties().valueEditor().set(new ValueEditor()))
					.dispatch();
		}
	}

	class ValueEditor extends Model.All implements ContextResolver.Has {
		class Resolver extends DelegatingContextResolver
				implements NodeEditorContext.Has {
			class NodeEditorContextImpl implements NodeEditorContext {
				@Override
				public boolean isEditable() {
					return true;
				}

				@Override
				public boolean isRenderAsNodeEditors() {
					return true;
				}
			}

			Resolver(ContextResolver logicalParent) {
				super(logicalParent);
			}

			@Override
			@Property.Not
			public NodeEditorContext getNodeEditorContext() {
				return new NodeEditorContextImpl();
			}
		}

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
		@Property.Not
		public ContextResolver getContextResolver(AnnotationLocation location) {
			return new Resolver(Searchable.this.provideNode().getResolver());
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
}