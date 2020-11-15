package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.actions.SelfPerformer;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

public class TaskPublishContentRequest
		implements SelfPerformer<TaskPublishContentRequest> {
	private ContentRequestBase request;

	public TaskPublishContentRequest() {
	}

	public TaskPublishContentRequest(ContentRequestBase request) {
		this.request = request;
	}

	public ContentRequestBase getRequest() {
		return this.request;
	}

	@Override
	public void performAction(TaskPublishContentRequest task) throws Exception {
		// FIXME - mvcc.4 - don't go via impl.publich
		Registry.impl(CommonRemoteServiceServlet.class)
				.publish(task.getRequest());
	}

	public void setRequest(ContentRequestBase request) {
		this.request = request;
	}
}