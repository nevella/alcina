package cc.alcina.framework.servlet.schedule;

import cc.alcina.framework.common.client.actions.SelfPerformer;
import cc.alcina.framework.common.client.job.Task;
import cc.alcina.framework.servlet.knowns.KnownJob;

public abstract class ServerTask<T extends Task> implements SelfPerformer<T> {
	protected String value;

	public String getValue() {
		return this.value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	protected KnownJob getKnownJob() {
		return null;
	}
}
