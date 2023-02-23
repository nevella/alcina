package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Arrays;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.impl.FocusImpl;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.Binding.DefinesProperties;
import com.totsp.gwittir.client.beans.BindingBuilder;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.csobjects.BaseSourcesPropertyChangeEvents;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.csobjects.HasChanges;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.common.client.reflection.Property;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.gwt.client.dirndl.activity.DirectedActivity;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.event.LayoutEvents.Bind;
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
 *
 * <p>
 * FIXME - dirndl 1x1d.0 - flatten hierarchy - remove nested subclasses,
 * BoundValues -> Bindings
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
 * @author nick@alcina.cc
 *
 */
@ObjectPermissions(
	read = @Permission(access = AccessLevel.EVERYONE),
	write = @Permission(access = AccessLevel.EVERYONE))
public abstract class Model extends Bindable implements
		LayoutEvents.Bind.Handler, LayoutEvents.BeforeRender.Handler, HasNode {
	private DirectedLayout.Node node;

	private Bindings bindings;

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
	 * @author nick@alcina.cc
	 *
	 */
	public Bindings bindings() {
		if (bindings == null) {
			bindings = new Bindings();
		}
		return bindings;
	}

	@Override
	public void onBeforeRender(BeforeRender event) {
		if (bindings != null) {
			bindings.setLeft();
		}
	}

	@Override
	/**
	 * Note that subclasses -must- call super.onBind(Bind) if overriding this
	 * event
	 */
	public void onBind(Bind event) {
		if (event.isBound()) {
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
			// TODO - nope, asymmetrical. Move to bindings
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

	/*
	 * This class supports 'fieldless' mode, where the property values are
	 * stored in the values map
	 *
	 * TODO - make that a subclass,since rare
	 */
	public class Bindings {
		private Binding binding = new Binding();

		public DetachList detachList = new DetachList();

		private Map<String, Object> values = AlcinaCollections.newUnqiueMap();

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

		public void add(Object leftPropertyName, Converter leftToRightConverter,
				SourcesPropertyChangeEvents right, Object rightPropertyName,
				Converter rightToLeftConverter) {
			SourcesPropertyChangeEvents left = getSource();
			add(left, leftPropertyName, leftToRightConverter, right,
					rightPropertyName, rightToLeftConverter);
		}

		/*
		 * Recomputes on any property change
		 */
		public void add(Object leftPropertyName,
				SourcesPropertyChangeEvents right) {
			BaseSourcesPropertyChangeEvents source = (BaseSourcesPropertyChangeEvents) getSource();
			RemovablePropertyChangeListener listener = new RemovablePropertyChangeListener(
					right, null, evt -> {
						source.firePropertyChange(null, evt.getOldValue(),
								evt.getNewValue());
					});
			// FIXME - dirndl 1x1d - route everything via detachlist /
			// hasbind
			listener.bind();
			detachList.add(listener);
		}

		public void add(Object leftPropertyName,
				SourcesPropertyChangeEvents right, Object rightPropertyName) {
			add(leftPropertyName, null, right, rightPropertyName, null);
		}

		public void add(SourcesPropertyChangeEvents left,
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
		}

		public void add(SourcesPropertyChangeEvents left,
				Object leftPropertyName, SourcesPropertyChangeEvents right,
				Object rightPropertyName) {
			add(left, leftPropertyName, null, right, rightPropertyName, null);
		}

		public <I, O> void addOneway(Object leftPropertyName,
				SourcesPropertyChangeEvents right, Object rightPropertyName,
				Converter<I, O> rightToLeftConverter) {
			add(leftPropertyName, Binding.IGNORE_CHANGE, right,
					rightPropertyName, rightToLeftConverter);
		}

		public void bind() {
			binding.bind();
		}

		public boolean isFieldless() {
			return this.fieldless;
		}

		public void setFieldless(boolean fieldless) {
			this.fieldless = fieldless;
		}

		public void setLeft() {
			binding.setLeft();
		}

		public void unbind() {
			binding.unbind();
			detachList.detach();
		}

		public <T> T value(Object propertyName) {
			String propertyNameString = PropertyEnum
					.asPropertyName(propertyName);
			return (T) values.get(propertyNameString);
		}

		private SourcesPropertyChangeEvents getSource() {
			SourcesPropertyChangeEvents left = fieldless ? propertyChangeSource
					: Model.this;
			return left;
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

	public interface FocusOnBind {
		boolean isFocusOnBind();

		default void onBind(FocusOnBind dispatchMarker, Bind event) {
			if (event.isBound() && isFocusOnBind()) {
				// definitely deferred (not finally), since the dom can be
				// mutated in finally blocks
				Scheduler.get().scheduleDeferred(() -> {
					Widget widget = event.getContext().node.getWidget();
					FocusImpl.getFocusImplForWidget()
							.focus(widget.getElement());
				});
			}
		}
	}

	public interface RerouteBubbledEvents {
		Model rerouteBubbledEventsTo();
	}
}
