package cc.alcina.framework.common.client.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.util.AlcinaTopics;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.StringPair;

@Bean
@MappedSuperclass
public class ClientLogRecord implements Serializable {
	static final transient long serialVersionUID = -3L;

	public static final String VALUE_SEPARATOR = "\tvalue :: ";

	public static StringPair parseLocationValue(String str) {
		int idx = str.indexOf(VALUE_SEPARATOR);
		return idx == -1 ? new StringPair(str, null)
				: new StringPair(str.substring(0, idx),
						str.substring(idx + VALUE_SEPARATOR.length()));
	}

	private int localSeriesId;

	private String clientInstanceAuth;

	private long clientInstanceId;

	private Date time;

	private String topic;

	private String message;

	private String ipAddress;

	public ClientLogRecord() {
	}

	public ClientLogRecord(int localSeriesId, String clientInstanceAuth,
			long clientInstanceId, Date time, String topic, String message,
			String ipAddr) {
		this.localSeriesId = localSeriesId;
		this.clientInstanceAuth = clientInstanceAuth;
		this.clientInstanceId = clientInstanceId;
		this.time = time;
		this.topic = topic;
		this.message = message;
		this.ipAddress = ipAddr;
	}

	public String getClientInstanceAuth() {
		return this.clientInstanceAuth;
	}

	public long getClientInstanceId() {
		return this.clientInstanceId;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public int getLocalSeriesId() {
		return this.localSeriesId;
	}

	@Transient
	@Lob
	public String getMessage() {
		return this.message;
	}

	public Date getTime() {
		return this.time;
	}

	public String getTopic() {
		return this.topic;
	}

	public void setClientInstanceAuth(String clientInstanceAuth) {
		this.clientInstanceAuth = clientInstanceAuth;
	}

	public void setClientInstanceId(long clientInstanceId) {
		this.clientInstanceId = clientInstanceId;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public void setLocalSeriesId(int id) {
		this.localSeriesId = id;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	@Override
	public String toString() {
		return Ax.format("%s :: %s :: %s :: %s -- %s", getLocalSeriesId(),
				getTime(), getClientInstanceId(), getTopic(),
				CommonUtils.trimToWsChars(getMessage(), 40));
	}

	public static class ClientLogRecordIsNonCriticalFilter
			implements Predicate<ClientLogRecord> {
		@Override
		public boolean test(ClientLogRecord o) {
			if (o.topic == null || o.message == null) {
				return false;
			}
			return o.topic.equals(AlcinaTopics.LOG_CATEGORY_HISTORY)
					|| o.topic.equals(AlcinaTopics.LOG_CATEGORY_CLICK)
					|| o.topic.equals(AlcinaTopics.LOG_CATEGORY_STAT)
					|| o.topic.equals(AlcinaTopics.LOG_CATEGORY_METRIC);
		}
	}

	public static class ClientLogRecordKeepNonCriticalPrecedingContextFilter
			implements Predicate<ClientLogRecord> {
		@Override
		public boolean test(ClientLogRecord o) {
			if (o.topic == null || o.message == null) {
				return false;
			}
			return o.topic.equals(AlcinaTopics.LOG_CATEGORY_EXCEPTION);
		}
	}

	@Bean
	public static class ClientLogRecords implements Serializable {
		static final transient long serialVersionUID = -3L;

		private List<ClientLogRecord> logRecords = new ArrayList<ClientLogRecord>();

		public int size = 0;

		public String buf = "";

		public void addLogRecord(ClientLogRecord logRecord) {
			logRecords.add(logRecord);
			buf += TransformManager.serialize(logRecord);
			incrementSize(logRecord);
		}

		public List<ClientLogRecord> getLogRecords() {
			return this.logRecords;
		}

		private void incrementSize(ClientLogRecord logRecord) {
			size += 70 + logRecord.message.length();
		}

		public void recalcSize() {
			size = 0;
			for (ClientLogRecord logRecord : logRecords) {
				incrementSize(logRecord);
			}
		}

		public void setLogRecords(List<ClientLogRecord> logRecords) {
			this.logRecords = logRecords;
		}

		@Override
		public String toString() {
			return Ax.format("ClientLogRecords: size - %s\t records - %s\n%s",
					size, logRecords.size(),
					CommonUtils.join(logRecords, "\n"));
		}
	}
}
