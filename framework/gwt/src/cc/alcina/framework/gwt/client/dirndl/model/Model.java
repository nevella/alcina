package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Arrays;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.gwittir.validator.NotBlankValidator;
import cc.alcina.framework.common.client.logic.RemovablePropertyChangeListener;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.gwt.client.dirndl.annotation.Directed;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormModelTransformer;

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
@Bean
@ObjectPermissions(read = @Permission(access = AccessLevel.EVERYONE), write = @Permission(access = AccessLevel.EVERYONE))
public abstract class Model extends Bindable {
	public static final transient Object MODEL_UPDATED = new Object();

	public void fireUpdated() {
		fireUnspecifiedPropertyChange(MODEL_UPDATED);
	}

	public void unbind() {
		Arrays.stream(propertyChangeSupport().getPropertyChangeListeners())
				.filter(pcl -> pcl instanceof RemovablePropertyChangeListener)
				.forEach(pcl -> ((RemovablePropertyChangeListener) pcl)
						.unbind());
	}

	@FormModelTransformer.Args(focusOnAttach = "string")
	public static class EditableStringModel extends Model {
		private String string;

		public EditableStringModel() {
		}

		public EditableStringModel(String string) {
			this.string = string;
		}

		@Display(name = "String")
		@Validator(validator = NotBlankValidator.class)
		public String getString() {
			return this.string;
		}

		public void setString(String string) {
			String old_string = this.string;
			this.string = string;
			propertyChangeSupport().firePropertyChange("string", old_string,
					string);
		}
	}

	@Directed(tag = "div")
	public static class StringModel extends Model {
		private String string;

		public StringModel() {
		}

		public StringModel(String string) {
			this.string = string;
		}

		@Directed
		public String getString() {
			return this.string;
		}

		public void setString(String string) {
			this.string = string;
		}
	}
}
