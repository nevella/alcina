package cc.alcina.framework.common.client.job;

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

	public String provideHtmlLog() {
		String regex = "(?s)^INFO.{0,50}((?:<\\?xml|<html).*)";
		return getActionLog().replaceFirst(regex, "$1");
	}

	public void setProducedObject(Object producedObject) {
		this.producedObject = producedObject;
	}
}
