package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Arrays;

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
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.behaviour.GwtEvents;
import cc.alcina.framework.gwt.client.dirndl.behaviour.GwtEvents.Attach;
import cc.alcina.framework.gwt.client.dirndl.layout.DirectedLayout;

/**
 * Thoughts on binding :: particularly in the case of UI bindings, a.b->c.d is
 * _sometimes_ better handled via "listen to major updates on a" - this
 * simplifies the handling of "a changed" vs "a.b changed".
 *
 * This is the motivation for the fireUpdated() method, see particularly
 * DirectedSingleEntityActivity
 *
 * @author nick@alcina.cc
 *
 */
@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.EVERYONE))
public abstract class Model extends Bindable {
	public static final transient Object MODEL_UPDATED = new Object();

	public void bind() {
	}

	public void fireUpdated() {
		fireUnspecifiedPropertyChange(MODEL_UPDATED);
	}

	public void unbind() {
		if (!hasPropertyChangeSupport()) {
			return;
		}
		Arrays.stream(propertyChangeSupport().getPropertyChangeListeners())
				.filter(pcl -> pcl instanceof RemovablePropertyChangeListener)
				.forEach(pcl -> ((RemovablePropertyChangeListener) pcl)
						.unbind());
	}

	/*
	 * There's a bit of an overuse of "binding" here - the superclass binds
	 * properties to the rendered object (generally dom element), this binds
	 * bean properties using Gwittir bindings
	 */
	public static class WithBinding extends Model {
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
		public void bind() {
			binding.bind();
			binding.setLeft();
			super.bind();
		}

		@Override
		public void unbind() {
			binding.unbind();
			super.unbind();
		}
	}

	@Directed(receives = GwtEvents.Attach.class)
	// No mixins
	public static class WithBindingAndNode extends Model.WithBinding
			implements GwtEvents.Attach.Handler {
		protected DirectedLayout.Node node;

		@Override
		public void onAttach(Attach event) {
			if (event.isAttached()) {
				node = event.getContext().node;
			} else {
				node = null;
			}
		}
	}

	@Directed(receives = GwtEvents.Attach.class)
	public static class WithNode extends Model
			implements GwtEvents.Attach.Handler {
		protected DirectedLayout.Node node;

		@Override
		public void onAttach(Attach event) {
			if (event.isAttached()) {
				node = event.getContext().node;
			} else {
				node = null;
			}
		}
	}
}
