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

@Bean(displayNamePropertyName = "", allPropertiesVisualisable = true)
@ObjectPermissions(read = @Permission(access = AccessLevel.ADMIN), write = @Permission(access = AccessLevel.ADMIN))
public class ServerControlParams extends Bindable implements RemoteParameters {
	private String propertyValue;

	private String propertyName;

	private boolean runGc;

	private boolean serialiseTestObjects;

	private String loggerName;

	private LogLevel logLevel = LogLevel.DEBUG;

	@GwtTransient
	private double doubleParam = 0;

	@Display(name = "my fave double", orderingHint = 90)
	@Validators(validators = {
			@Validator(validator = InstantiableDoubleValidator.class),
			@Validator(validator = NotNullValidator.class) })
	public double getDoubleParam() {
		return this.doubleParam;
	}

	@Display(name = "Change logger - name", orderingHint = 40)
	public String getLoggerName() {
		return this.loggerName;
	}

	@Display(name = "Change logger - level", orderingHint = 45)
	public LogLevel getLogLevel() {
		return logLevel;
	}

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

	@Display(name = "Run GC", orderingHint = 30)
	public boolean isRunGc() {
		return this.runGc;
	}

	@Display(name = "Serialise test objects", orderingHint = 15)
	public boolean isSerialiseTestObjects() {
		return this.serialiseTestObjects;
	}

	public void setDoubleParam(double doubleParam) {
		double old_doubleParam = this.doubleParam;
		this.doubleParam = doubleParam;
		propertyChangeSupport().firePropertyChange("doubleParam",
				old_doubleParam, doubleParam);
	}

	public void setLoggerName(String loggerName) {
		String old_loggerName = this.loggerName;
		this.loggerName = loggerName;
		propertyChangeSupport().firePropertyChange("loggerName", old_loggerName,
				loggerName);
	}

	public void setLogLevel(LogLevel logLevel) {
		LogLevel old_logLevel = this.logLevel;
		this.logLevel = logLevel;
		propertyChangeSupport().firePropertyChange("logLevel", old_logLevel,
				logLevel);
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

	public void setRunGc(boolean runGc) {
		boolean old_runGc = this.runGc;
		this.runGc = runGc;
		propertyChangeSupport().firePropertyChange("runGc", old_runGc, runGc);
	}

	public void setSerialiseTestObjects(boolean serialiseTestObjects) {
		boolean old_serialiseTestObjects = this.serialiseTestObjects;
		this.serialiseTestObjects = serialiseTestObjects;
		propertyChangeSupport().firePropertyChange("serialiseTestObjects",
				old_serialiseTestObjects, serialiseTestObjects);
	}
}
