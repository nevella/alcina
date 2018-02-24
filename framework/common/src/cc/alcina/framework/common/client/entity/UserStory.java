package cc.alcina.framework.common.client.entity;

import javax.persistence.Lob;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import cc.alcina.framework.common.client.csobjects.AbstractDomainBase;
import cc.alcina.framework.common.client.logic.reflection.DomainTransformPersistable;
import cc.alcina.framework.common.client.util.CommonUtils;

@MappedSuperclass
@DomainTransformPersistable
public abstract class UserStory<U extends UserStory> extends AbstractDomainBase<U> {
	protected long id;

	private String trigger;

	private String story;

	private long clientInstanceId;

	private String clientInstanceUid;

	public UserStory() {
		super();
	}

	public long getClientInstanceId() {
		return this.clientInstanceId;
	}

	public String getClientInstanceUid() {
		return this.clientInstanceUid;
	}

	@Lob
	@Transient
	public String getStory() {
		return this.story;
	}

	public String getTrigger() {
		return this.trigger;
	}

	public void setClientInstanceId(long clientInstanceId) {
		long old_clientInstanceId = this.clientInstanceId;
		this.clientInstanceId = clientInstanceId;
		propertyChangeSupport().firePropertyChange("clientInstanceId",
				old_clientInstanceId, clientInstanceId);
	}

	public void setClientInstanceUid(String clientInstanceUid) {
		String old_clientInstanceUid = this.clientInstanceUid;
		this.clientInstanceUid = clientInstanceUid;
		propertyChangeSupport().firePropertyChange("clientInstanceUid",
				old_clientInstanceUid, clientInstanceUid);
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setStory(String story) {
		String old_story = this.story;
		this.story = story;
		propertyChangeSupport().firePropertyChange("story", old_story, story);
	}

	public void setTrigger(String trigger) {
		String old_trigger = this.trigger;
		this.trigger = trigger;
		propertyChangeSupport().firePropertyChange("trigger", old_trigger,
				trigger);
	}

	@Override
	public String toString() {
		return CommonUtils.formatJ("%s : %s", clientInstanceId, trigger);
	}
}