package cc.alcina.framework.common.client.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import com.totsp.gwittir.client.beans.annotations.Introspectable;

import cc.alcina.framework.common.client.logic.reflection.BeanInfo;
import cc.alcina.framework.common.client.logic.reflection.ClientInstantiable;
import cc.alcina.framework.common.client.util.AlcinaBeanSerializer;
import cc.alcina.framework.common.client.util.StringPair;

@BeanInfo(displayNamePropertyName = "time")
@Introspectable
@MappedSuperclass
public class ClientLogRecord implements Serializable {
	private int localSeriesId;

	private String clientInstanceAuth;

	private long clientInstanceId;

	private Date time;

	private String topic;

	private String message;

	private String ipAddress;

	public static final String VALUE_SEPARATOR = "\tvalue :: ";

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

	@BeanInfo(displayNamePropertyName = "size")
	@Introspectable
	public static class ClientLogRecords {
		private List<ClientLogRecord> logRecords = new ArrayList<ClientLogRecord>();

		public int size = 0;

		public String buf = "";

		public void addLogRecord(ClientLogRecord logRecord) {
			logRecords.add(logRecord);
			buf += new AlcinaBeanSerializer().serialize(logRecord);
			size += 70 + logRecord.message.length();
		}

		public List<ClientLogRecord> getLogRecords() {
			return this.logRecords;
		}

		public void setLogRecords(List<ClientLogRecord> logRecords) {
			this.logRecords = logRecords;
		}
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public static StringPair parseLocationValue(String str) {
		int idx = str.indexOf(VALUE_SEPARATOR);
		return idx == -1 ? new StringPair(str, null) : new StringPair(
				str.substring(0, idx), str.substring(idx
						+ VALUE_SEPARATOR.length()));
	}
}
