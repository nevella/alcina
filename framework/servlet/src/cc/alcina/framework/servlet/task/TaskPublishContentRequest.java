package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;
import cc.alcina.framework.servlet.schedule.ServerTask;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

@TypeSerialization(notSerializable = true)
public class TaskPublishContentRequest
		extends ServerTask<TaskPublishContentRequest> {
	private ContentRequestBase request;

	public TaskPublishContentRequest() {
	}

	public TaskPublishContentRequest(ContentRequestBase request) {
		this.request = request;
	}

	public ContentRequestBase getRequest() {
		return this.request;
	}

	public void setRequest(ContentRequestBase request) {
		this.request = request;
	}

	@Override
	protected void performAction0(TaskPublishContentRequest task)
			throws Exception {
		// FIXME - mvcc.4 - don't go via impl.publich
		Registry.impl(CommonRemoteServiceServlet.class)
				.publish(task.getRequest());
	}
}