package cc.alcina.framework.common.client.entity;

import java.util.Date;
import java.util.List;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;

public interface IUserStory<U extends IUserStory> extends HasIdAndLocalId {
	String getCart();

	long getClientInstanceId();

	String getClientInstanceUid();

	Date getDate();

	String getEmail();

	String getHttpReferrer();

	@Override
	long getId();

	String getIid();

	String getLocation();

	List<ClientLogRecord> getLogs();

	String getStory();

	String getTrigger();

	String getUserAgent();

	void setCart(String cart);

	void setClientInstanceId(long clientInstanceId);

	void setClientInstanceUid(String clientInstanceUid);

	void setDate(Date date);

	void setEmail(String email);

	void setHttpReferrer(String httpReferrer);

	@Override
	void setId(long id);

	void setIid(String iid);

	void setLocation(String location);

	void setLogs(List<ClientLogRecord> logs);

	void setStory(String story);

	void setTrigger(String trigger);

	void setUserAgent(String userAgent);
}