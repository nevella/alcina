package cc.alcina.framework.entity.logic;

import java.util.Date;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;

public class AlcinaServerConfig {
	public static final String MAIN_LOGGER_APPENDER = "MAIN_LOGGER_APPENDER";

	public static AlcinaServerConfig get() {
		return Registry.impl(AlcinaServerConfig.class);
	}

	private String mainLoggerName;

	private String applicationName;

	private String customPropertiesFilePath;
	
	private String metricLoggerName;

	private String databaseEventLoggerName;

	private Date startDate;

	public String getApplicationName() {
		return applicationName;
	}

	public String getCustomPropertiesFilePath() {
		return customPropertiesFilePath;
	}

	public String getDatabaseEventLoggerName() {
		return databaseEventLoggerName;
	}

	public String getMainLoggerName() {
		return mainLoggerName;
	}

	public String getMetricLoggerName() {
		return metricLoggerName;
	}

	public Date getStartDate() {
		return this.startDate;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public void setCustomPropertiesFilePath(String customPropertiesFileName) {
		this.customPropertiesFilePath = customPropertiesFileName;
	}

	public void setDatabaseEventLoggerName(String databaseEventLoggerName) {
		this.databaseEventLoggerName = databaseEventLoggerName;
	}

	public void setMainLoggerName(String mainLoggerName) {
		this.mainLoggerName = mainLoggerName;
	}

	public void setMetricLoggerName(String metricLoggerName) {
		this.metricLoggerName = metricLoggerName;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
}
