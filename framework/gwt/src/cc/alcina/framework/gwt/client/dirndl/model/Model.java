package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Arrays;

import com.totsp.gwittir.client.beans.Binding;
import com.totsp.gwittir.client.beans.BindingBuilder;
import com.totsp.gwittir.client.beans.SourcesPropertyChangeEvents;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyEnum;

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
		Arrays.stream(propertyChangeSupport().getPropertyChangeListeners())
				.filter(pcl -> pcl instanceof RemovablePropertyChangeListener)
				.forEach(pcl -> ((RemovablePropertyChangeListener) pcl)
						.unbind());
	}

	public static class WithBinding extends Model {
		Binding binding = new Binding();

		public void addBinding(Object fromPropertyName,
				SourcesPropertyChangeEvents to, Object toPropertyName) {
			String fromPropertyNameString = PropertyEnum
					.asPropertyName(fromPropertyName);
			String toPropertyNameString = PropertyEnum
					.asPropertyName(toPropertyName);
			Binding child = BindingBuilder.bind(this)
					.onLeftProperty(fromPropertyNameString).toRight(to)
					.onRightProperty(toPropertyNameString).toBinding();
			binding.getChildren().add(child);
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
}
