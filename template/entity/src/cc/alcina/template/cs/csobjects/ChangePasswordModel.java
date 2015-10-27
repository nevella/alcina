package cc.alcina.template.cs.csobjects;

import cc.alcina.framework.common.client.actions.RemoteParameters;
import cc.alcina.framework.common.client.csobjects.BaseBindable;
import cc.alcina.framework.common.client.gwittir.validator.ParameterisedStringLengthValidator;
import cc.alcina.framework.common.client.gwittir.validator.StringHasLengthValidator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.CustomiserInfo;
import cc.alcina.framework.common.client.logic.reflection.DisplayInfo;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.PropertyPermissions;
import cc.alcina.framework.common.client.logic.reflection.ValidatorInfo;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.logic.reflection.VisualiserInfo;
import cc.alcina.framework.gwt.client.gwittir.customiser.PasswordCustomiser;

@BeanInfo(displayNamePropertyName = "null")
@ObjectPermissions(read = @Permission(access = AccessLevel.LOGGED_IN), write = @Permission(access = AccessLevel.LOGGED_IN))
public class ChangePasswordModel extends BaseBindable implements RemoteParameters{
	private String newPassword;

	private String newPassword2;

	private Long userId;

	@DisplayInfo(name = "New password", orderingHint = 2)
	@PropertyPermissions(read = @Permission(access = AccessLevel.LOGGED_IN), write = @Permission(access = AccessLevel.LOGGED_IN))
	@CustomiserInfo(customiserClass = PasswordCustomiser.class)
	@Validators(validators = { @ValidatorInfo(validator = ParameterisedStringLengthValidator.class, parameters = {
			@NamedParameter(name = ValidatorInfo.FEEDBACK_MESSAGE, stringValue = "Minimum of 5 characters in length"),
			@NamedParameter(name = ParameterisedStringLengthValidator.MIN_CHARS, intValue = 5) }) })
	public String getNewPassword() {
		return this.newPassword;
	}

	@DisplayInfo(name = "New password (confirm)", orderingHint = 3)
	@PropertyPermissions(read = @Permission(access = AccessLevel.LOGGED_IN), write = @Permission(access = AccessLevel.LOGGED_IN))
	@CustomiserInfo(customiserClass = PasswordCustomiser.class)
	@Validators(validators = { @ValidatorInfo(validator = StringHasLengthValidator.class) })
	// dummy, validator has to be set by code
	public String getNewPassword2() {
		return this.newPassword2;
	}

	public Long getUserId() {
		return userId;
	}

	public void setNewPassword(String newPassword) {
		String old_newPassword = this.newPassword;
		this.newPassword = newPassword;
		propertyChangeSupport().firePropertyChange("newPassword",
				old_newPassword, newPassword);
	}

	public void setNewPassword2(String newPassword2) {
		String old_newPassword2 = this.newPassword2;
		this.newPassword2 = newPassword2;
		propertyChangeSupport().firePropertyChange("newPassword2",
				old_newPassword2, newPassword2);
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}
}