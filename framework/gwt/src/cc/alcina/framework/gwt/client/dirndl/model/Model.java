package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Arrays;

import com.google.gwt.dom.client.Element;
import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.BindingBuilder;
import com.totsp.gwittir.client.beans.Converter;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;
import cc.alcina.framework.gwt.client.dirndl.behaviour.LayoutEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.LayoutEvents.BeforeRender;
import cc.alcina.framework.gwt.client.dirndl.behaviour.LayoutEvents.Bind;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;

/**
 * <p>
 * Thoughts on binding :: particularly in the case of UI bindings, a.b->c.d is
 * _sometimes_ better handled via "listen to major updates on a" - this
 * simplifies the handling of "a changed" vs "a.b changed".
 *
 * <p>
 * This is the motivation for the fireUpdated() method, see particularly
 * DirectedSingleEntityActivity
 *
 * <p>
 * Note - above comments were early dirndl days - almost certainly that
 * mechanism can be replaced by a ModelEvent (possibly causing Model
 * replacement) - at which point remove
 *
 * @author nick@alcina.cc
 *
 */
@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.EVERYONE))
public abstract class Model extends Bindable
		implements LayoutEvents.Bind.Handler {
	public static final transient Object MODEL_UPDATED = new Object();

	public void fireUpdated() {
		fireUnspecifiedPropertyChange(MODEL_UPDATED);
	}

	@Override
	/**
	 * Note that subclasses must call super.onBind(Bind) if overriding this
	 * event
	 */
	public void onBind(Bind event) {
		if (!event.isBound()) {
			if (!hasPropertyChangeSupport()) {
				return;
			}
			Arrays.stream(propertyChangeSupport().getPropertyChangeListeners())
					.filter(pcl -> pcl instanceof RemovablePropertyChangeListener)
					.forEach(pcl -> ((RemovablePropertyChangeListener) pcl)
							.unbind());
		}
	}

	/*
	 * There's a bit of an overuse of "binding" here - the superclass binds
	 * properties to the rendered object (generally dom element), this binds
	 * bean properties using Gwittir bindings
	 */
	public static class WithBinding extends Model
			implements LayoutEvents.BeforeRender.Handler {
		private Binding binding = new Binding();

		public void addBinding(Object leftPropertyName,
				Converter leftToRightConverter,
				SourcesPropertyChangeEvents right, Object rightPropertyName,
				Converter rightToLeftConverter) {
			String leftPropertyNameString = PropertyEnum
					.asPropertyName(leftPropertyName);
			String rightPropertyNameString = PropertyEnum
					.asPropertyName(rightPropertyName);
			Binding child = BindingBuilder.bind(this)
					.onLeftProperty(leftPropertyNameString)
					.convertLeftWith(leftToRightConverter).toRight(right)
					.onRightProperty(rightPropertyNameString)
					.convertRightWith(rightToLeftConverter).toBinding();
			binding.getChildren().add(child);
		}

		public void addBinding(Object leftPropertyName,
				SourcesPropertyChangeEvents right, Object rightPropertyName) {
			addBinding(leftPropertyName, null, right, rightPropertyName, null);
		}

		@Override
		public void onBeforeRender(BeforeRender event) {
			binding.setLeft();
		}

		@Override
		public void onBind(Bind event) {
			if (event.isBound()) {
				binding.bind();
			} else {
				binding.unbind();
			}
			super.onBind(event);
		}
	}

	// No mixins (although this effectively mixes WithNode + WithBinding)
	public static class WithBindingAndNode extends Model.WithBinding {
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
	}

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
}
