package cc.alcina.framework.servlet.task;

import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.Permissions;
import cc.alcina.framework.common.client.logic.permissions.Permissions.LoginState;
import cc.alcina.framework.common.client.logic.permissions.UserlandProvider;
import cc.alcina.framework.common.client.publication.request.ContentRequestBase;
import cc.alcina.framework.common.client.publication.request.PublicationResult;
import cc.alcina.framework.common.client.serializer.TypeSerialization;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.persistence.mvcc.Transaction;
import cc.alcina.framework.servlet.job.JobContext;
import cc.alcina.framework.servlet.schedule.PerformerTask;

@TypeSerialization(flatSerializable = false)
public class TaskPublish extends PerformerTask implements ServletAwaitTask {
	public static String extractDownloadUrl(String response) {
		return response.replaceFirst(
				"(?s).+Added download at (/downloadServlet.do\\?id=.+)\n.+",
				"$1");
	}

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
		return getClass().getSimpleName() + " - "
				+ getPublicationRequest().provideJobName();
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

	@Override
	public void run() throws Exception {
		PublicationResult result = null;
		IUser user = JobContext.get().getJob().getUser();
		if (user == UserlandProvider.get().getSystemUser()) {
			result = publicationRequest.publish();
		} else {
			try {
				Permissions.pushUser(user, LoginState.LOGGED_IN);
				result = publicationRequest.publish();
			} finally {
				Permissions.popUser();
			}
		}
		if (copyContentToLargeResult || Ax.isTest()) {
			JobContext.get().getJob().setLargeResult(result.getContent());
		}
		if (!Ax.isTest()) {
			result.ensureMinimal();
		}
		JobContext.get().getJob().setResult(result);
		Transaction.commit();
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
}
