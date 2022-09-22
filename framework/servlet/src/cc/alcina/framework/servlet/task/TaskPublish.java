package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationRequestHandler;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.schedule.ServerTask;

@TypeSerialization(flatSerializable = false)
public class TaskPublish extends ServerTask<TaskPublish>
		implements ServletAwaitTask {
	private ContentRequestBase publicationRequest;

	private boolean copyContentToLargeResult;

	private boolean awaitJobCompletion;

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

	@Override
	public boolean isAwaitJobCompletion() {
		return this.awaitJobCompletion;
	}

	public boolean isCopyContentToLargeResult() {
		return this.copyContentToLargeResult;
	}

	public void setAwaitJobCompletion(boolean awaitJobCompletion) {
		this.awaitJobCompletion = awaitJobCompletion;
	}

	public void setCopyContentToLargeResult(boolean copyContentToLargeResult) {
		this.copyContentToLargeResult = copyContentToLargeResult;
	}

	public void setPublicationRequest(ContentRequestBase publicationRequest) {
		this.publicationRequest = publicationRequest;
	}

	public ServletAwaitTask withRequest(ContentRequestBase publicationRequest) {
		setPublicationRequest(publicationRequest);
		return this;
	}

	@Override
	protected void performAction0(TaskPublish task) throws Exception {
		PublicationResult result = Registry
				.impl(PublicationRequestHandler.class)
				.publish(getPublicationRequest());
		if (copyContentToLargeResult) {
			JobContext.get().getJob().setLargeResult(result.getContent());
		}
		if (!Ax.isTest()) {
			result.ensureMinimal();
		}
		JobContext.get().getJob().setResult(result);
		Transaction.commit();
	}
}
