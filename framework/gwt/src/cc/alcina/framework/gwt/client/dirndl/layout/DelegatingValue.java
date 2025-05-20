package cc.alcina.framework.gwt.client.dirndl.layout;

import java.util.function.Function;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.layout.BridgingValueRenderer.ValueResolver;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel;
import cc.alcina.framework.gwt.client.dirndl.model.Model;
import cc.alcina.framework.gwt.client.dirndl.model.ValueTransformer;

/*
 * Renders all non-editable fields (by default) as their 'natural' renderer.
 * 
 * FIXME - dirndl - high - customisation (via valuetransformer) this is very
 * much unoptimised
 */
@Directed.Delegating
@Registration({ Model.Value.class, FormModel.Viewer.class, Object.class })
@TypeSerialization(flatSerializable = false, reflectiveSerializable = false)
public class DelegatingValue extends Model.Value<Object>
		implements ContextResolver.Has {
	Object value;

	Class<? extends Function> valueTransformer;

	@Override
	@Property.Not
	public ContextResolver getContextResolver(AnnotationLocation location) {
		location.optional(ValueTransformer.class).ifPresent(
				ann -> valueTransformer = (Class<? extends ModelTransform>) ann
						.value());
		if (valueTransformer == null) {
			return null;
		} else {
			return new ValueTransformerResolver();
		}
	}

	/*
	 * Ideally we'd use a ContextResolver.AnnotationCustomiser - but the
	 * resolution path here is complex, so we imperatively apply the model.
	 * 
	 * FIXME - dirndl 3.0
	 */
	class ValueTransformerResolver extends ContextResolver {
		ValueTransformerResolver() {
		}

		@Override
		protected BindingsCache bindingsCache() {
			ValueResolver typedParent = (ValueResolver) parent;
			return typedParent.bindingsCache();
		}

		@Override
		protected Object resolveModel(AnnotationLocation location,
				Object model) {
			if (model == value) {
				return Reflections.newInstance(valueTransformer).apply(model);
			}
			return super.resolveModel(location, model);
		}
	}

	@Override
	@Directed
	public Object getValue() {
		return this.value;
	}

	@Override
	public void setValue(Object value) {
		set("value", this.value, value, () -> this.value = value);
	}
}