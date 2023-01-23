package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Arrays;
import java.util.Map;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Element;
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
 *
 * @author nick@alcina.cc
 *
 */
@ObjectPermissions(
	read = @Permission(access = AccessLevel.EVERYONE),
	write = @Permission(access = AccessLevel.EVERYONE))
public abstract class Model extends Bindable
		implements LayoutEvents.Bind.Handler {
	@Override
	/**
	 * Note that subclasses must call super.onBind(Bind) if overriding this
	 * event
	 */
	public void onBind(Bind event) {
		if (event.isBound()) {
			// I'm not sure that this is the best way to dispatch, but there may
			// be no other way
			// to call arbitrary interface methods non-reflectively?
			if (this instanceof FocusOnBind) {
				FocusOnBind focusOnBind = (FocusOnBind) this;
				focusOnBind.onBind(focusOnBind, event);
			}
		} else {
			if (!hasPropertyChangeSupport()) {
				return;
			}
			Arrays.stream(propertyChangeSupport().getPropertyChangeListeners())
					.filter(pcl -> pcl instanceof RemovablePropertyChangeListener)
					.forEach(pcl -> ((RemovablePropertyChangeListener) pcl)
							.unbind());
		}
	}

	public interface FocusOnBind {
		boolean isFocusOnBind();

		default void onBind(FocusOnBind dispatchMarker, Bind event) {
			if (event.isBound() && isFocusOnBind()) {
				Scheduler.get().scheduleFinally(() -> {
					Widget widget = event.getContext().node.getWidget();
					FocusImpl.getFocusImplForWidget()
							.focus(widget.getElement());
				});
			}
		}
	}

	/**
	 * This extension of Model exposes the corresponding Node in the dirdnl
	 * layout tree. It's mostly used to provide access to the rendered DOM for
	 * things like focus, scroll and rendered offset handling.
	 *
	 * @author nick@alcina.cc
	 *
	 */
	public static class WithNode extends Model {
		protected DirectedLayout.Node node;

		@Override
		public void onBind(Bind event) {
			if (event.isBound()) {
				node = event.getContext().node;
			} else {
				node = null;
			}
			super.onBind(event);
		}

		public Element provideElement() {
			return node.getWidget().getElement();
		}
	}

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
	 * @author nick@alcina.cc
	 *
	 */
	public static class WithPropertyBinding extends Model
			implements LayoutEvents.BeforeRender.Handler {
		private BoundValues bindings = new BoundValues();

		public BoundValues bindings() {
			return bindings;
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			bindings.setLeft();
		}

		@Override
		public void onBind(Bind event) {
			if (event.isBound()) {
				bindings.bind();
			} else {
				bindings.unbind();
			}
			super.onBind(event);
		}

		public class BoundValues {
			private Binding binding = new Binding();

			private Map<String, Object> values = AlcinaCollections
					.newUnqiueMap();

			/*
			 * keep the fieldless propertychange contract mostly separate/clean
			 *
			 */
			private PropertyChangeSource propertyChangeSource = new PropertyChangeSource();

			/*
			 * model has no field-backed properties, instead all values are
			 * stored in the values array (and getters call
			 * BoundValues.value(propertyName))
			 */
			private boolean fieldless;

			public void add(Object leftPropertyName,
					Converter leftToRightConverter,
					SourcesPropertyChangeEvents right, Object rightPropertyName,
					Converter rightToLeftConverter) {
				String leftPropertyNameString = PropertyEnum
						.asPropertyName(leftPropertyName);
				String rightPropertyNameString = PropertyEnum
						.asPropertyName(rightPropertyName);
				SourcesPropertyChangeEvents left = fieldless
						? propertyChangeSource
						: WithPropertyBinding.this;
				Binding child = BindingBuilder.bind(left)
						.onLeftProperty(leftPropertyNameString)
						.convertLeftWith(leftToRightConverter).toRight(right)
						.onRightProperty(rightPropertyNameString)
						.convertRightWith(rightToLeftConverter).toBinding();
				binding.getChildren().add(child);
			}

			public void add(Object leftPropertyName,
					SourcesPropertyChangeEvents right,
					Object rightPropertyName) {
				add(leftPropertyName, null, right, rightPropertyName, null);
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
					WithPropertyBinding.this.firePropertyChange(getName(),
							oldValue, newValue);
				}
			}

			class PropertyChangeSource extends BaseSourcesPropertyChangeEvents
					implements DefinesProperties {
				@Override
				public Property provideProperty(String propertyName) {
					Property property = Reflections.at(WithPropertyBinding.this)
							.property(propertyName);
					return new MapBackedProperty(property);
				}
			}
		}
	}

	// No mixins sez Java (so this effectively mixes WithNode + WithBinding)
	public static class WithPropertyBindingAndNode
			extends Model.WithPropertyBinding {
		protected DirectedLayout.Node node;

		@Override
		public void onBind(Bind event) {
			if (event.isBound()) {
				node = event.getContext().node;
			} else {
				node = null;
			}
			super.onBind(event);
		}

		public Element provideElement() {
			return node.getWidget().getElement();
		}
	}
}
