package cc.alcina.template.cs.misc.search;

import cc.alcina.framework.common.client.csobjects.LogMessageType;
import cc.alcina.framework.common.client.search.EnumCriterion;
public class LogMessageTypeEnumCriterion extends EnumCriterion<LogMessageType> {
	static final transient long serialVersionUID = -1L;
	private LogMessageType logMessageType;

	public LogMessageTypeEnumCriterion() {
	}

	public LogMessageTypeEnumCriterion(
			String criteriaDisplayName, boolean withNull) {
		super( criteriaDisplayName, withNull);
	}

	public void setLogMessageType(LogMessageType logMessageType) {
		LogMessageType old_logMessageType = this.logMessageType;
		this.logMessageType = logMessageType;
		propertyChangeSupport().firePropertyChange("logMessageType",
				old_logMessageType, logMessageType);
	}

	public LogMessageType getLogMessageType() {
		return logMessageType;
	}


	@Override
	protected boolean valueAsString() {
		return true;
	}

	@Override
	public LogMessageType getValue() {
		return getLogMessageType();
	}

	@Override
	public void setValue(LogMessageType value) {
		setLogMessageType(value);
	}
}
