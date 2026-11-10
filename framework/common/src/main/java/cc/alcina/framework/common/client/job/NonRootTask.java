package cc.alcina.framework.common.client.job;

import cc.alcina.framework.common.client.logic.permissions.IUser;

public interface NonRootTask extends Task {
	default IUser provideIUser(Job job) {
		return job.getUser();
	}
}
