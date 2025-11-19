package cc.alcina.framework.gwt.client.dirndl.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import cc.alcina.framework.common.client.logic.reflection.reachability.ClientVisible;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.Client;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.Change;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvents.SelectionChanged;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.model.HasNode;

/**
 * A marker interface UI-binding events (events for which the change value could
 * be a new value for the dirndl transform source property )
 */
public interface ValueChange {
	default Object getNewValue() {
		return ((ModelEvent) this).getModel();
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@ClientVisible
	public @interface Bind {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@Documented
	@Target({ ElementType.METHOD, ElementType.FIELD })
	@ClientVisible
	public @interface BindingRequired {
	}

	/**
	 * <p>
	 * A container which receives ValueChange implementation events, and (if the
	 * container has a @BindingRequired annotation, the originating property
	 * must have a @Bind annotation) sets the property value to the the change
	 * value
	 */
	public interface Container extends ModelEvents.SelectionChanged.Handler,
			ModelEvents.Change.Handler {
		@Override
		default void onSelectionChanged(SelectionChanged event) {
			if (event.checkReemitted((HasNode) this)) {
				event.bubble();
				return;
			}
			Support.propagateChange(this, event);
		}

		@Override
		default void onChange(Change event) {
			if (event.checkReemitted((HasNode) this)) {
				event.bubble();
				return;
			}
			Support.propagateChange(this, event);
		}

		static class Support {
			static void propagateChange(Container container, ModelEvent event) {
				DirectedLayout.Node previousNode = event.getContext()
						.getPrevious().node;
				// SKY - it would be more accurate to ensure that
				// previousNode.model is a transformation of container.property
				// -- but that requires more kit on Node.model ancestry (which
				// Dirndl tries to avoid). This check is good enough to avoid
				// 99.9% of propagated transformations
				Property property = previousNode
						.getAnnotationLocation().property;
				boolean bind = property.getOwningType() == container.getClass();
				bind &= !Reflections.at(container).has(BindingRequired.class)
						|| previousNode.has(Bind.class);
				if (bind) {
					ValueChange valueChange = (ValueChange) event;
					Object newValue = valueChange.getNewValue();
					/*
					 * Because this property may be bound, enqueue the change
					 */
					Client.eventBus().queued()
							.lambda(() -> property.set(container, newValue))
							.dispatch();
				} else {
					event.bubble();
				}
			}
		}
	}
}
