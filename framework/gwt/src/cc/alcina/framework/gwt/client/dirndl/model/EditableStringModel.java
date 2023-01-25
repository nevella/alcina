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

	@Display
	@Validator(validator = NotBlankValidator.class)
	@Custom(
		customiserClass = TextAreaCustomiser.class,
		parameters = {
				@NamedParameter(
					name = TextAreaCustomiser.ENSURE_ALL_LINES_VISIBLE,
					booleanValue = true),
				@NamedParameter(
					name = TextAreaCustomiser.LINES,
					intValue = 1) })
	public String getString() {
		return this.string;
	}

	public void setString(String string) {
		String old_string = this.string;
		this.string = string;
		propertyChangeSupport().firePropertyChange("string", old_string,
				string);
	}

	public static class SingleLine extends EditableStringModel {
		public SingleLine() {
		}

		public SingleLine(String string) {
			super(string);
		}

		@Override
		@Display
		@Validator(validator = NotBlankValidator.class)
		public String getString() {
			return super.getString();
		}
	}

	public static class SingleLineArea extends EditableStringModel {
		public SingleLineArea() {
		}

		public SingleLineArea(String string) {
			super(string);
		}

		@Override
		@Display
		@Custom(
			customiserClass = TextAreaCustomiser.class,
			parameters = {
					@NamedParameter(
						name = TextAreaCustomiser.ENSURE_ALL_LINES_VISIBLE,
						booleanValue = false),
					@NamedParameter(
						name = TextAreaCustomiser.LINES,
						intValue = 1) })
		public String getString() {
			return super.getString();
		}
	}
}
