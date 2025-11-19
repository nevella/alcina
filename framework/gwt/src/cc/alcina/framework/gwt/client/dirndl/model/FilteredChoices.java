package cc.alcina.framework.gwt.client.dirndl.model;

import java.beans.PropertyChangeEvent;
import java.lang.annotation.Annotation;
import java.util.List;

import cc.alcina.framework.common.client.logic.reflection.resolution.AnnotationLocation;
import cc.alcina.framework.common.client.reflection.TypedProperties;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents;
import cc.alcina.framework.gwt.client.dirndl.event.ValueChange;
import cc.alcina.framework.gwt.client.dirndl.layout.ContextResolver;
import cc.alcina.framework.gwt.client.dirndl.layout.HandlesModelChange;
import cc.alcina.framework.gwt.client.dirndl.layout.ModelTransform;
import cc.alcina.framework.gwt.client.dirndl.model.edit.StringInput;

@TypedProperties
public class FilteredChoices<T> extends Model.Fields
		implements ModelEvents.Filter.Emitter, ContextResolver.Has,
		ValueChange.Container, HandlesModelChange {
	public static class To implements ModelTransform<List, FilteredChoices<?>> {
		@Override
		public FilteredChoices<?> apply(List value) {
			FilteredChoices<List> filteredChoices = new FilteredChoices<>();
			filteredChoices.value = value;
			// filteredChoices.setSelectedValues(t);
			return filteredChoices;
		}
	}

	static class Resolver extends ContextResolver {
		AnnotationLocation rootLocation;

		Resolver(AnnotationLocation location) {
			this.rootLocation = location;
		}

		/*
		 * resolve annotations on the FilterChoices source if not on the
		 * descendant
		 */
		@Override
		protected <A extends Annotation> List<A> resolveAnnotations0(
				Class<A> annotationClass, AnnotationLocation location) {
			List<A> result = super.resolveAnnotations0(annotationClass,
					location);
			if (result != null && result.size() > 0) {
				return result;
			} else {
				return super.resolveAnnotations0(annotationClass, rootLocation);
			}
		}
	}

	class Filter extends Model.All {
		@Directed(
			reemits = { ModelEvents.Input.class, ModelEvents.Filter.class })
		@Directed.Transform(value = StringInput.To.class, transformsNull = true)
		@StringInput.FocusOnBind
		String filter;
	}

	@Directed
	Filter filter = new Filter();

	@Directed.Transform(Choices.Multiple.To.class)
	List<T> value;

	@Override
	public void onBeforeRender(BeforeRender event) {
		/*
		 * must reemit selectionchanged events for parent container update
		 */
		from(properties().value())
				.emitStreamElement(ModelEvents.SelectionChanged.class);
		super.onBeforeRender(event);
	}

	@Override
	public ContextResolver getContextResolver(AnnotationLocation location) {
		return new Resolver(location);
	}

	PackageProperties._FilteredChoices.InstanceProperties properties() {
		return PackageProperties.filteredChoices.instance(this);
	}

	@Override
	public boolean handlesModelChange(PropertyChangeEvent evt) {
		properties().value().set((List) evt.getNewValue());
		return true;
	}
}
