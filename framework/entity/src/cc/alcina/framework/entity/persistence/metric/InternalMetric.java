package cc.alcina.framework.entity.persistence.metric;

import java.util.Date;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.DomainTransformPersistable;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation;
import cc.alcina.framework.common.client.logic.domain.DomainTransformPropagation.PropagationType;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.JsonObjectSerializer;
import cc.alcina.framework.entity.persistence.metric.InternalMetrics.BlackboxData;
import cc.alcina.framework.entity.projection.GraphProjection;
import cc.alcina.framework.entity.util.JacksonJsonObjectSerializer;
import cc.alcina.framework.entity.util.JacksonUtils;

@MappedSuperclass
@DomainTransformPersistable
@DomainTransformPropagation(PropagationType.NON_PERSISTENT)
@TypeSerialization(reflectiveSerializable = false)
public abstract class InternalMetric<U extends InternalMetric>
		extends Entity<U> {
	private String threadName;

	private String callName;

	private Date startTime;

	private Date updateTime;

	private Date endTime;

	private String obfuscatedArgs;

	private int sliceCount;

	private String sliceJson;

	private String hostName;

	private String lockType;

	private String blackboxData;

	private long clientInstanceId;

	private transient ThreadHistory threadHistory;

	public InternalMetric() {
	}

	@Lob
	@Transient
	public String getBlackboxData() {
		return this.blackboxData;
	}

	public String getCallName() {
		return this.callName;
	}

	public long getClientInstanceId() {
		return this.clientInstanceId;
	}

	public Date getEndTime() {
		return this.endTime;
	}

	public String getHostName() {
		return this.hostName;
	}

	public String getLockType() {
		return this.lockType;
	}

	@Lob
	@Transient
	public String getObfuscatedArgs() {
		return this.obfuscatedArgs;
	}

	public int getSliceCount() {
		return this.sliceCount;
	}

	@Lob
	@Transient
	public String getSliceJson() {
		return this.sliceJson;
	}

	public Date getStartTime() {
		return this.startTime;
	}

	@Transient
	public ThreadHistory getThreadHistory() {
		if (threadHistory == null) {
			threadHistory = new ThreadHistory();
			if (sliceJson != null) {
				try {
					threadHistory = new JacksonJsonObjectSerializer()
							.withAllowUnknownProperties()
							.deserialize(sliceJson, ThreadHistory.class);
				} catch (Exception e) {
					threadHistory.note = "Unable to deserialize";
				}
			}
		}
		return this.threadHistory;
	}

	public String getThreadName() {
		return this.threadName;
	}

	public Date getUpdateTime() {
		return this.updateTime;
	}

	public BlackboxData provideBlackboxData() {
		return JacksonUtils.deserialize(blackboxData, BlackboxData.class);
	}

	public void setBlackboxData(String blackboxData) {
		this.blackboxData = blackboxData;
	}

	public void setCallName(String callName) {
		this.callName = callName;
	}

	public void setClientInstanceId(long clientInstanceId) {
		this.clientInstanceId = clientInstanceId;
	}

	public void setEndTime(Date end) {
		this.endTime = end;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	@Override
	public void setId(long id) {
		super.setId(id);
	}

	public void setLockType(String lockType) {
		this.lockType = lockType;
	}

	public void setObfuscatedArgs(String obfuscatedArgs) {
		this.obfuscatedArgs = obfuscatedArgs;
	}

	public void setSliceCount(int sliceCount) {
		this.sliceCount = sliceCount;
	}

	public void setSliceJson(String sliceJson) {
		this.sliceJson = sliceJson;
	}

	public void setStartTime(Date start) {
		this.startTime = start;
	}

	public void setThreadHistory(ThreadHistory threadHistory) {
		this.threadHistory = threadHistory;
		if (threadHistory != null) {
			setSliceJson(
					JsonObjectSerializer.get().serializeNoThrow(threadHistory));
		}
	}

	public void setThreadName(String threadName) {
		this.threadName = threadName;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public String toString() {
		return GraphProjection.fieldwiseToStringOneLine(this);
	}
}