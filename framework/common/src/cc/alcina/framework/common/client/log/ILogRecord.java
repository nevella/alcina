package cc.alcina.framework.common.client.log;

import java.util.Date;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface ILogRecord extends HasIdAndLocalId {
	public static final String COMPONENT_KEY = "componentKey";

	public static final String USER_ID = "userId";

	Long getClientInstanceId();

	String getComponentKey();

	Date getCreatedOn();

	String getData();

	String getHost();

	String getIpAddress();

	String getText();

	Long getUserId();

	String getUserName();

	void setClientInstanceId(Long clientInstanceId);

	void setComponentKey(String componentKey);

	void setCreatedOn(Date createdOn);

	void setData(String data);

	void setHost(String host);

	void setIpAddress(String ipAddress);

	void setText(String text);

	void setUserId(Long userId);

	void setUserName(String userName);
}