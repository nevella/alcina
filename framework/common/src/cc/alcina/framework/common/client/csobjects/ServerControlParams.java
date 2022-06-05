package cc.alcina.framework.common.client.csobjects;

import com.google.gwt.user.client.rpc.GwtTransient;

import cc.alcina.framework.common.client.actions.RemoteParameters;
import cc.alcina.framework.common.client.gwittir.validator.InstantiableDoubleValidator;
import cc.alcina.framework.common.client.gwittir.validator.NotNullValidator;
import cc.alcina.framework.common.client.logic.domaintransform.spi.AccessLevel;
import cc.alcina.framework.common.client.logic.reflection.Custom;
import cc.alcina.framework.common.client.logic.reflection.Display;
import cc.alcina.framework.common.client.logic.reflection.NamedParameter;
import cc.alcina.framework.common.client.logic.reflection.ObjectPermissions;
import cc.alcina.framework.common.client.logic.reflection.Permission;
import cc.alcina.framework.common.client.logic.reflection.Validator;
import cc.alcina.framework.common.client.logic.reflection.Validators;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.gwt.client.gwittir.customiser.TextAreaCustomiser;
import cc.alcina.framework.gwt.client.logic.LogLevel;

@Bean@Display.AllProperties
@ObjectPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN))
public class ServerControlParams extends Bindable implements RemoteParameters {
	private String propertyValue;

	private String propertyName;

	@Display(name = "App property name/Task FQN", orderingHint = 20, styleName = "wide-text", focus = true)
	public String getPropertyName() {
		return this.propertyName;
	}

	@Custom(customiserClass = TextAreaCustomiser.class, parameters = {
			@NamedParameter(name = TextAreaCustomiser.WIDTH, intValue = 400),
			@NamedParameter(name = TextAreaCustomiser.LINES, intValue = 15) })
	@Display(name = "App property value/Task param", orderingHint = 25, styleName = "wide-text")
	public String getPropertyValue() {
		return this.propertyValue;
	}

	public void setPropertyName(String propertyName) {
		String old_propertyName = this.propertyName;
		this.propertyName = propertyName;
		propertyChangeSupport().firePropertyChange("propertyName",
				old_propertyName, propertyName);
	}

	public void setPropertyValue(String propertyValue) {
		String old_propertyValue = this.propertyValue;
		this.propertyValue = propertyValue;
		propertyChangeSupport().firePropertyChange("propertyValue",
				old_propertyValue, propertyValue);
	}
}
