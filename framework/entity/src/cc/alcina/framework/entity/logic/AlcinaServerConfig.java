package cc.alcina.framework.entity.logic;

public class AlcinaServerConfig {
	private String mainLoggerName;

	private String applicationName;
	
	private String customPropertiesFilePath;
	
	private String metricLoggerName;
	
	private String databaseEventLoggerName;

	public static final String MAIN_LOGGER_APPENDER = "MAIN_LOGGER_APPENDER";
	
	public void setMainLoggerName(String mainLoggerName) {
		this.mainLoggerName = mainLoggerName;
	}

	public String getMainLoggerName() {
		return mainLoggerName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public String getApplicationName() {
		return applicationName;
	}
	private AlcinaServerConfig() {
		super();
	}

	private static AlcinaServerConfig theInstance;

	public static AlcinaServerConfig get() {
		if (theInstance == null) {
			theInstance = new AlcinaServerConfig();
		}
		return theInstance;
	}

	public void setCustomPropertiesFilePath(String customPropertiesFileName) {
		this.customPropertiesFilePath = customPropertiesFileName;
	}

	public String getCustomPropertiesFilePath() {
		return customPropertiesFilePath;
	}

	public void setMetricLoggerName(String metricLoggerName) {
		this.metricLoggerName = metricLoggerName;
	}

	public String getMetricLoggerName() {
		return metricLoggerName;
	}

	public void setDatabaseEventLoggerName(String databaseEventLoggerName) {
		this.databaseEventLoggerName = databaseEventLoggerName;
	}

	public String getDatabaseEventLoggerName() {
		return databaseEventLoggerName;
	}

	
}
