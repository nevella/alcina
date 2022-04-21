package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.schedule.ServerTask;
import cc.alcina.framework.servlet.servlet.PublicationRequestHandler;

@TypeSerialization(flatSerializable = false)
public class TaskPublish extends ServerTask<TaskPublish> {
	private ContentRequestBase publicationRequest;

	public TaskPublish() {
	}

	public TaskPublish(ContentRequestBase publicationRequest) {
		setPublicationRequest(publicationRequest);
	}

	@Override
	public String getName() {
		return getPublicationRequest().provideJobName();
	}

	public ContentRequestBase getPublicationRequest() {
		return this.publicationRequest;
	}

	public void setPublicationRequest(ContentRequestBase publicationRequest) {
		this.publicationRequest = publicationRequest;
	}

	public TaskPublish withRequest(ContentRequestBase publicationRequest) {
		setPublicationRequest(publicationRequest);
		return this;
	}

	@Override
	protected void performAction0(TaskPublish task) throws Exception {
		PublicationResult result = Registry
				.impl(PublicationRequestHandler.class)
				.publish(getPublicationRequest());
		if (!Ax.isTest()) {
			result.ensureMinimal();
		}
		JobContext.get().getJob().setResult(result);
		Transaction.commit();
	}
}
