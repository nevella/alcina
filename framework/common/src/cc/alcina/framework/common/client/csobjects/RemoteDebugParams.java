package cc.alcina.framework.common.client.csobjects;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.actions.RemoteParameters;
import cc.alcina.framework.common.client.gwittir.validator.InstantiableDoubleValidator;
import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Bean;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.gwt.client.gwittir.customiser.TextAreaCustomiser;
import cc.alcina.framework.gwt.client.logic.LogLevel;

@Bean( allPropertiesVisualisable = true)
@ObjectPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN))
public class RemoteDebugParams extends Bindable implements RemoteParameters {
	private String command;

	@Display(name = "Command", orderingHint = 20, styleName = "wide-text", focus = true)
	public String getCommand() {
		return this.command;
	}

	public void setCommand(String command) {
		String old_command = this.command;
		this.command = command;
		propertyChangeSupport().firePropertyChange("command", old_command, command);
		
	}
}
