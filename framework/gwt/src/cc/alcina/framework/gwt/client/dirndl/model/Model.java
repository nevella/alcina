package cc.alcina.framework.gwt.client.dirndl.model;

import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import com.google.web.bindery.event.shared.HandlerRegistration;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Binding.DefinesProperties;
import com.totsp.gwittir.client.beans.BindingBuilder;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.HasChanges;
import cc.alcina.framework.common.client.logic.ListenerBinding;
import cc.alcina.framework.common.client.logic.ListenerBindings;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.domain.HasValue;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.event.ModelEvent;
import cc.alcina.framework.gwt.client.dirndl.event.NodeEvent;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout.Node;

/**
 * <p>
 * Dirndl UI models are mostly composed of subclasses of this class - they're
 * abstract (Java Beans + extra sauce).
 *
 *
 *
 * <h3>Notes</h3>
 *
 *
 * <p>
 * Thoughts on binding :: particularly in the case of UI bindings, a.b->c.d is
 * _sometimes_ better handled via "listen to major updates on a" - this
 * simplifies the handling of "a changed" vs "a.b changed".
 *
 * <p>
 * This is the motivation for - for instance - {@link DirectedActivity}
 * implementing {@link HasChanges}. Originally model itself provided a 'whole
 * object changed' event support' - but it's rarely used, so moved to the
 * subclasses that need it.
 * <p>
 * Normally standard propertychange events will work to update a UI (or - in
 * general - a model transformation) when the model changes, but that breaks
 * down when the transformation is more than one layer deep - an example would
 * be a dirndl @Transform model transformation of say a User object to a
 * UserView model - say User.firstName changes and UserView.name is
 * (firstName-space-lastName), how should that change flow be observed in a way
 * that causes the UserView to change? One simplistic answer is to have UserView
 * implement HasChanges, and fire topicChanged().signal() on UserView whenever
 * an input change (e.g. any property of User) is observed. That's a blunt
 * instrument, but in the absence of more granular (propertychange) binding
 * possibilities, it's reasonable.
 * <p>
 * ...some time later - HasChanges was removed from DirectedActivity - of
 * course, we have the mechanism! ModelEvents...
 *
 *
 * <p>
 * DOC - bindings
 *
 * <p>
 * Laziness: bindings is lazy but dependent fields are not. Simple RO models
 * (table, tree leaves - where the vast majority of instances are in a large UI)
 * won't use it, so that's the simplest + most effective (memory-conserving)
 * optimisation
 *
 * <p>
 * Serialization: this is predominantly a UI class, if you need a just a general
 * purpose base reflective serializable class use Bindable (or any Bean-like
 * with an @Bean annotation (FIXME - beans 2)). That's not to say that this
 * shouldn't be used as an rpc class (when sending server-side renderable model
 * trees/graphs to the client), it just shouldn't be the go-to base.
 *
 *
 *
 */
@ObjectPermissions(
	read = @Permission(access = AccessLevel.EVERYONE),
	write = @Permission(access = AccessLevel.EVERYONE))
public abstract class Model extends Bindable implements
		LayoutEvents.Bind.Handler, LayoutEvents.BeforeRender.Handler, HasNode {
	private transient DirectedLayout.Node node;

	private transient Bindings bindings;

	/**
	 * <p>
	 * Adds support for lifecycle binding of model properties to other objects.
	 * The property bindings (which cascade property changes with optional
	 * validation and transformation) are set up/torn down during the model
	 * onBind event, so are only live (and reachable from the GC point of view)
	 * while the model is attached to the layout tree.
	 *
	 * <p>
	 * FIXME - dirndl 1x1h - document/exemplify binding types (field-backed,
	 * non-field-backed)
	 *
	 * <p>
	 * When to setup the bindings? Either in the constructor or the subclass
	 * onBeforeRender handler *before* the super call. First time they're used
	 * is in this class's {@code onBeforeRender} method
	 *
	 *
	 *
	 */
	public Bindings bindings() {
		if (bindings == null) {
			bindings = new Bindings();
		}
		return bindings;
	}

	public void emitEvent(Class<? extends ModelEvent> clazz) {
		emitEvent(clazz, this);
	}

	public void emitEvent(Class<? extends ModelEvent> clazz, Object value) {
		if (!provideIsBound()) {
			return;
		}
		NodeEvent.Context.fromNode(provideNode()).dispatch(clazz, value);
	}

	/**
	 * Subclasses should call super.onBeforeRender at the *end* of their binding
	 * setup (generally at the end of the method)
	 */
	@Override
	public void onBeforeRender(BeforeRender event) {
		if (bindings != null) {
			bindings.setLeft();
		}
	}

	@Override
	/**
	 * Note that subclasses -must- call super.onBind(Bind) if overriding this
	 * event - and generally should call it first (since it sets
	 * {@code model.node}). Bindings should be set up prior to this call
	 */
	public void onBind(Bind event) {
		if (event.isBound()) {
			if (node != null) {
				Ax.err("binding a model to multiple nodes.\n"
						+ "--------------------------\n" + "Existing node:\n%s"
						+ "\n--------------------------\n"
						+ "Incoming node:\n%s", node.toParentStack(),
						event.getContext().node.toParentStack());
				Preconditions.checkState(node == null);
			}
			node = event.getContext().node;
			if (bindings != null) {
				bindings.bind();
			}
			// I'm not sure that this is the best way to dispatch, but there may
			// be no other way
			// to call arbitrary interface methods non-reflectively?
			if (this instanceof FocusOnBind) {
				FocusOnBind focusOnBind = (FocusOnBind) this;
				focusOnBind.onBind(focusOnBind, event);
			}
		} else {
			if (bindings != null) {
				bindings.unbind();
			}
			node = null;
			if (!hasPropertyChangeSupport()) {
				return;
			}
			// FIXME - dirndl - low (not sure if this is accessed) - nope,
			// asymmetrical. Move to bindings
			Arrays.stream(propertyChangeSupport().getPropertyChangeListeners())
					.filter(pcl -> pcl instanceof RemovablePropertyChangeListener)
					.forEach(pcl -> ((RemovablePropertyChangeListener) pcl)
							.unbind());
		}
	}

	/*
	 * Provide access to the corresponding Node in the dirdnl layout tree. It's
	 * mostly used to provide access to the rendered DOM for things like focus,
	 * scroll and rendered offset handling.
	 */
	@Override
	public Node provideNode() {
		return node;
	}

	@Directed.AllProperties
	public static abstract class All extends Fields {
	}

	/*
	 * This class supports 'fieldless' mode, where the property values are
	 * stored in the values map
	 *
	 * TODO - make that a subclass,since rare
	 * 
	 * FIXME - dirndl - simplify to just from(SPCE/TOPIC) and
	 * add(listenerbinding)
	 */
	public class Bindings {
		private Binding binding = new Binding();

		private Map<String, Object> values = AlcinaCollections.newUnqiueMap();

		private ListenerBindings listenerBindings = new ListenerBindings();

		/*
		 * keep the fieldless propertychange contract mostly separate/clean
		 *
		 */
		private PropertyChangeSource propertyChangeSource = new PropertyChangeSource();

		/*
		 * model has no field-backed properties, instead all values are stored
		 * in the values array (and getters call
		 * BoundValues.value(propertyName))
		 */
		private boolean fieldless;

		private boolean bound;

		public List<ModelBinding> modelBindings = new ArrayList<>();

		public Binding add(Object leftPropertyName,
				Converter leftToRightConverter,
				SourcesPropertyChangeEvents right, Object rightPropertyName,
				Converter rightToLeftConverter) {
			SourcesPropertyChangeEvents left = getSource();
			return add(left, leftPropertyName, leftToRightConverter, right,
					rightPropertyName, rightToLeftConverter);
		}

		public Binding add(Object leftPropertyName,
				SourcesPropertyChangeEvents right, Object rightPropertyName) {
			return add(leftPropertyName, null, right, rightPropertyName, null);
		}

		public Binding add(SourcesPropertyChangeEvents left,
				Object leftPropertyName, Converter leftToRightConverter,
				SourcesPropertyChangeEvents right, Object rightPropertyName,
				Converter rightToLeftConverter) {
			String leftPropertyNameString = PropertyEnum
					.asPropertyName(leftPropertyName);
			String rightPropertyNameString = PropertyEnum
					.asPropertyName(rightPropertyName);
			Binding child = BindingBuilder.bind(left)
					.onLeftProperty(leftPropertyNameString)
					.convertLeftWith(leftToRightConverter).toRight(right)
					.onRightProperty(rightPropertyNameString)
					.convertRightWith(rightToLeftConverter).toBinding();
			binding.getChildren().add(child);
			return child;
		}

		public void add(SourcesPropertyChangeEvents left,
				Object leftPropertyName, SourcesPropertyChangeEvents right,
				Object rightPropertyName) {
			add(left, leftPropertyName, null, right, rightPropertyName, null);
		}

		public void addBinding(Binding childBinding) {
			binding.getChildren().add(childBinding);
		}

		/**
		 * Add a ListenerBinding (often a RemovablePropertyChangeListener which
		 * fires on a model property change) e.g:
		 *
		 * <pre>
		 * <code>
		 * bindings().addListener(new RemovablePropertyChangeListener(
		 * this, "value", e -> select.setSelectedValue(value)));
		 * </code>
		 * </pre>
		 */
		public void addListener(ListenerBinding listenerBinding) {
			listenerBindings.add(listenerBinding);
		}

		public void addListener(
				Supplier<ListenerReference> listenerReferenceSupplier) {
			listenerBindings.add(listenerReferenceSupplier);
		}

		public <I, O> void addOneway(Object leftPropertyName,
				SourcesPropertyChangeEvents right, Object rightPropertyName) {
			addOneway(getSource(), leftPropertyName, right, rightPropertyName,
					null);
		}

		public <I, O> void addOneway(Object leftPropertyName,
				SourcesPropertyChangeEvents right, Object rightPropertyName,
				Converter<I, O> rightToLeftConverter) {
			addOneway(getSource(), leftPropertyName, right, rightPropertyName,
					rightToLeftConverter);
		}

		public <I, O> void addOneway(SourcesPropertyChangeEvents left,
				Object leftPropertyName, SourcesPropertyChangeEvents right,
				Object rightPropertyName,
				Converter<I, O> rightToLeftConverter) {
			add(left, leftPropertyName, Binding.IGNORE_CHANGE, right,
					rightPropertyName, rightToLeftConverter);
		}

		/**
		 * Add a consumer-form property change listener
		 *
		 * @return
		 */
		public RemovablePropertyChangeListener addPropertyChangeListener(
				SourcesPropertyChangeEvents bean, Object propertyName,
				Consumer<PropertyChangeEvent> consumer) {
			RemovablePropertyChangeListener listener = new RemovablePropertyChangeListener(
					bean, propertyName, e -> consumer.accept(e));
			addListener(listener);
			return listener;
		}

		/**
		 * Add a property change listener which does not inspect the event
		 *
		 * @return
		 */
		public RemovablePropertyChangeListener addPropertyChangeListener(
				SourcesPropertyChangeEvents bean, Object propertyName,
				Runnable runnable) {
			return addPropertyChangeListener(bean, propertyName,
					evt -> runnable.run());
		}

		public void addRegistration(
				Supplier<HandlerRegistration> handlerRegistrationSupplier) {
			listenerBindings.add(asBinding(handlerRegistrationSupplier));
		}

		/*
		 * This recomputes source.leftPropertyName on any property change to
		 * right. It does *not* create a 1-1 property binding
		 *
		 * in fact - this looks incorrectly implemented (leftPropertyName is
		 * unused)...possibly remove
		 */
		public void addUnspecified(Object leftPropertyName,
				SourcesPropertyChangeEvents right) {
			BaseSourcesPropertyChangeEvents source = (BaseSourcesPropertyChangeEvents) getSource();
			RemovablePropertyChangeListener listener = new RemovablePropertyChangeListener(
					right, null, evt -> {
						source.firePropertyChange(null, evt.getOldValue(),
								evt.getNewValue());
					});
			addListener(listener);
		}

		private ListenerBinding asBinding(
				Supplier<HandlerRegistration> handlerRegistrationSupplier) {
			return new ListenerBinding() {
				private HandlerRegistration reference;

				@Override
				public void bind() {
					reference = handlerRegistrationSupplier.get();
				}

				@Override
				public void unbind() {
					reference.removeHandler();
					reference = null;
				}
			};
		}

		public void bind() {
			Preconditions.checkState(!bound);
			binding.bind();
			listenerBindings.bind();
			modelBindings.forEach(ModelBinding::bind);
			bound = true;
		}

		public <T extends SourcesPropertyChangeEvents> ModelBinding<T>
				from(T source) {
			ModelBinding binding = new ModelBinding(this);
			modelBindings.add(binding);
			return binding.from(source);
		}

		public <TE> ModelBinding<TE> from(Topic<TE> topic) {
			ModelBinding binding = new ModelBinding(this);
			modelBindings.add(binding);
			return binding.from(topic);
		}

		private SourcesPropertyChangeEvents getSource() {
			SourcesPropertyChangeEvents left = fieldless ? propertyChangeSource
					: Model.this;
			return left;
		}

		public boolean isFieldless() {
			return this.fieldless;
		}

		Model model() {
			return Model.this;
		}

		public void setFieldless(boolean fieldless) {
			this.fieldless = fieldless;
		}

		public void setLeft() {
			binding.setLeft();
			modelBindings.forEach(ModelBinding::prepare);
		}

		public void unbind() {
			Preconditions.checkState(bound);
			modelBindings.forEach(ModelBinding::unbind);
			listenerBindings.unbind();
			binding.unbind();
			bound = false;
		}

		public <T> T value(Object propertyName) {
			String propertyNameString = PropertyEnum
					.asPropertyName(propertyName);
			return (T) values.get(propertyNameString);
		}

		public class MapBackedProperty extends Property {
			public MapBackedProperty(Property property) {
				super(property);
			}

			@Override
			public Object get(Object bean) {
				throw new UnsupportedOperationException();
			}

			@Override
			public void set(Object bean, Object newValue) {
				Object oldValue = value(getName());
				values.put(getName(), newValue);
				Model.this.firePropertyChange(getName(), oldValue, newValue);
			}
		}

		class PropertyChangeSource extends BaseSourcesPropertyChangeEvents
				implements DefinesProperties {
			@Override
			public Property provideProperty(String propertyName) {
				Property property = Reflections.at(Model.this)
						.property(propertyName);
				return new MapBackedProperty(property);
			}
		}
	}

	@Bean(PropertySource.FIELDS)
	public abstract static class Fields extends Model {
	}

	public interface FocusOnBind {
		boolean isFocusOnBind();

		default void onBind(FocusOnBind dispatchMarker, Bind event) {
			if (event.isBound() && isFocusOnBind()) {
				// definitely deferred (not finally), since the dom can be
				// mutated in finally blocks
				Scheduler.get().scheduleDeferred(() -> {
					FocusImpl.getFocusImplForWidget().focus(
							event.getContext().node.getRendered().asElement());
				});
			}
		}
	}

	public interface Has {
		Model provideModel();
	}

	public interface RerouteBubbledEvents {
		Model rerouteBubbledEventsTo();
	}

	public abstract static class Value<T> extends Model implements HasValue<T> {
	}
}
