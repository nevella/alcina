package cc.alcina.framework.gwt.client.dirndl.model;

import cc.alcina.framework.common.client.gwittir.validator.NotBlankValidator;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.gwt.client.gwittir.customiser.TextAreaCustomiser;

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
	@Custom(customiserClass = TextAreaCustomiser.class, parameters = {
			@NamedParameter(name = TextAreaCustomiser.ENSURE_ALL_LINES_VISIBLE, booleanValue = true),
			@NamedParameter(name = TextAreaCustomiser.LINES, intValue = 1)})
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