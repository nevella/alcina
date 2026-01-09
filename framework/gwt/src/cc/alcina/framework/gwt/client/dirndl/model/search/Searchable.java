package cc.alcina.framework.gwt.client.dirndl.model.search;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.common.client.search.SearchCriterion;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.BridgingValueRenderer;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.DelegatingContextResolver;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContext;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;

@TypedProperties
class Searchable extends Model.Fields {
	SearchCriterion searchCriterion;

	@Directed
	String name;

	@Directed
	ValueEditor valueEditor;

	Searchable(SearchCriterion searchCriterion) {
		this.searchCriterion = searchCriterion;
		this.name = searchCriterion.getDisplayName();
	}

	PackageProperties._Searchable.InstanceProperties properties() {
		return PackageProperties.searchable.instance(this);
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
			public void onChildBindingCreated(Binding binding) {
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
}