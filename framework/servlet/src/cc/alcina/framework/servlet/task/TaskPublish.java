package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.serializer.flat.TypeSerialization;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.schedule.ServerTask;
import cc.alcina.framework.servlet.servlet.CommonRemoteServiceServlet;

@TypeSerialization(flatSerializable = false)
public class TaskPublish extends ServerTask<TaskPublish> {
	private ContentRequestBase publicationRequest;

	public ContentRequestBase getPublicationRequest() {
		return this.publicationRequest;
	}

	public void setPublicationRequest(ContentRequestBase publicationRequest) {
		this.publicationRequest = publicationRequest;
	}

	@Override
	protected void performAction0(TaskPublish task) throws Exception {
		PublicationResult result = Registry
				.impl(CommonRemoteServiceServlet.class)
				.publish(getPublicationRequest());
		result.ensureMinimal();
		JobContext.get().getJob().setResult(result);
		Transaction.commit();
	}
}