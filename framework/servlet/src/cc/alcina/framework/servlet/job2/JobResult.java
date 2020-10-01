package cc.alcina.framework.servlet.job2;

import cc.alcina.framework.common.client.actions.ActionLogItem;

public class JobResult {
	private Object producedObject;

	public String getActionLog() {
		// TODO Auto-generated method stub
		return null;
	}

	public ActionLogItem getActionLogItem() {
		// TODO Auto-generated method stub
		return null;
	}

	public <T> T getProducedObject() {
		return (T) this.producedObject;
	}

	public void setProducedObject(Object producedObject) {
		this.producedObject = producedObject;
	}
}
