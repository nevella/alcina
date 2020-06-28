package cc.alcina.framework.entity.entityaccess.metric;

import java.util.Date;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.util.JsonObjectSerializer;
import cc.alcina.framework.entity.projection.GraphProjection;

@MappedSuperclass
@DomainTransformPersistable
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

	private transient ThreadHistory threadHistory;

	public InternalMetric() {
	}

	public String getCallName() {
		return this.callName;
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
					threadHistory = JsonObjectSerializer.get()
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

	public void setCallName(String callName) {
		this.callName = callName;
	}

	public void setEndTime(Date end) {
		this.endTime = end;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	@Override
	public void setId(long id) {
		this.id = id;
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