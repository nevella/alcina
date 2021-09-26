package cc.alcina.framework.gwt.client.dirndl.model;

import java.util.Objects;

import cc.alcina.framework.common.client.gwittir.validator.NotBlankValidator;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormModelTransformer;

@FormModelTransformer.Args(focusOnAttach = "string")
public class EditableStringModel extends Model {
	private String string;

	private boolean dirty;

	private String originalValue;

	public EditableStringModel() {
	}

	public EditableStringModel(String string) {
		this.string = string;
		this.originalValue = string;
	}

	@Display(name = "String")
	@Validator(validator = NotBlankValidator.class)
	public String getString() {
		return this.string;
	}

	public boolean isDirty() {
		return this.dirty;
	}

	public void setDirty(boolean dirty) {
		boolean old_dirty = this.dirty;
		this.dirty = dirty;
		propertyChangeSupport().firePropertyChange("dirty", old_dirty,
				dirty);
	}

	public void setString(String string) {
		String old_string = this.string;
		this.string = string;
		propertyChangeSupport().firePropertyChange("string", old_string,
				string);
		setDirty(!Objects.equals(string, originalValue));
	}
}