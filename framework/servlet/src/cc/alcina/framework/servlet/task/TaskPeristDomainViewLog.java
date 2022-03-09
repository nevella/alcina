package cc.alcina.framework.servlet.task;

import java.io.File;
import java.util.function.Function;

import cc.alcina.framework.common.client.csobjects.view.DomainViewNodeContent;
import cc.alcina.framework.common.client.logic.domaintransform.EntityLocator;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.entity.util.JacksonUtils;
import cc.alcina.framework.servlet.domain.view.DomainViews;
import cc.alcina.framework.servlet.domain.view.LiveTree;
import cc.alcina.framework.servlet.domain.view.LiveTree.ProcessLoggerImpl;
import cc.alcina.framework.servlet.schedule.ServerTask;

public class TaskPeristDomainViewLog
		extends ServerTask<TaskPeristDomainViewLog> {
	private EntityLocator rootEntity;

	public transient ProcessLoggerImpl processLoggerImpl;

	public EntityLocator getRootEntity() {
		return this.rootEntity;
	}

	@Override
	public void performAction0(TaskPeristDomainViewLog task) throws Exception {
		Function<LiveTree, String> lambda = liveTree -> {
			return liveTree.persistProcessLog();
		};
		DomainViewNodeContent.Request request = new DomainViewNodeContent.Request();
		request.setRoot(rootEntity);
		String path = DomainViews.get().submitLambda(request, lambda);
		processLoggerImpl = JacksonUtils.deserializeFromFile(new File(path),
				LiveTree.ProcessLoggerImpl.class);
		Ax.out(processLoggerImpl);
	}

	public void setRootEntity(EntityLocator rootEntity) {
		this.rootEntity = rootEntity;
	}
}