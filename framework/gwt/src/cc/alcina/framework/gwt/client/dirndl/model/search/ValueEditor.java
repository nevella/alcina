package cc.alcina.framework.gwt.client.dirndl.model.search;

import com.totsp.gwittir.client.ui.table.Field;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.reflection.InstanceProperty;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation.Resolver;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.NodeContext;
import cc.alcina.framework.gwt.client.dirndl.layout.BridgingValueRenderer;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.ValueModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.NodeEditorContextService;
import cc.alcina.framework.gwt.client.gwittir.BeanFields;

@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class ValueEditor extends Model.Fields {
	@Directed(renderer = BridgingValueRenderer.class)
	class ValueModelImpl implements ValueModel {
		@Override
		public Bindable getBindable() {
			return (Bindable) instanceProperty.source;
		}

		Field field;

		ValueModelImpl() {
			Property property = instanceProperty.property.getProperty();
			field = BeanFields.query().forClass(getBindable().getClass())
					.forPropertyName(property.name()).withEditable(true)
					.forMultipleWidgetContainer(false)
					.withValidationFeedbackProvider(
							new FormModel.ValidationFeedbackProvider())
					.withResolver(resolver).getField();
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

	@Directed
	ValueModelImpl value;

	@Property.Not
	InstanceProperty<?, ?> instanceProperty;

	@Property.Not
	Resolver resolver;

	public ValueEditor(InstanceProperty<?, ?> instanceProperty,
			AnnotationLocation.Resolver resolver) {
		this.instanceProperty = instanceProperty;
		this.resolver = resolver;
		value = new ValueModelImpl();
	}

	@Override
	public void onNodeContext(NodeContext event) {
		node.getResolver().registerService(NodeEditorContextService.class,
				NodeEditorContextService.Editable.INSTANCE);
	}
}