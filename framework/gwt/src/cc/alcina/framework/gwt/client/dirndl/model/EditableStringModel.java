package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.gwittir.validator.NotBlankValidator;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.gwt.client.dirndl.model.FormModel.FormModelTransformer;

@FormModelTransformer.Args(focusOnAttach = "string")
public class EditableStringModel extends Model {
	private String string;

	private String originalValue;

	public EditableStringModel() {
	}

	public EditableStringModel(String string) {
		this.string = string;
		this.originalValue = string;
	}

	public String getOriginalValue() {
		return this.originalValue;
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