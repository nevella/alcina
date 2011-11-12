package cc.alcina.framework.servlet.actionhandlers;

import java.util.Arrays;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.csobjects.JobInfo;
import cc.alcina.framework.common.client.logic.domaintransform.DTRSimpleSerialWrapper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest.DomainTransformRequestType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.gwt.client.action.DtrSimpleAdminPersistenceAction;
import cc.alcina.framework.servlet.ServletLayerLocator;
import cc.alcina.framework.servlet.job.JobRegistry;

@RegistryLocation(registryPoint = RemoteActionPerformer.class, targetClass = DtrSimpleAdminPersistenceAction.class)
public class DtrSimpleAdminPersistenceHandler implements
		RemoteActionPerformer<DtrSimpleAdminPersistenceAction> {
	private JobInfo jobInfo;

	private ActionLogItem commit(DTRSimpleSerialWrapper wrapper) {
		Logger logger = ServletLayerLocator.get().remoteActionLoggerProvider()
				.getLogger(this.getClass());
		ActionLogItem item = null;
		long t1 = System.currentTimeMillis();
		int docsImported = 0;
		jobInfo = JobRegistry.get().startJob(getClass(),
				SEUtilities.friendlyClassName(getClass()), null);
		try {
			item = ServletLayerLocator.get().commonPersistenceProvider()
					.getCommonPersistence()
					.getImplementation(ActionLogItem.class).newInstance();
			String t = wrapper.getText();
			wrapper.setDomainTransformRequestType(DomainTransformRequestType.TO_REMOTE);
			ServletLayerLocator
					.get()
					.commonRemoteServletProvider()
					.getCommonRemoteServiceServlet()
					.persistOfflineTransforms(
							Arrays.asList(new DTRSimpleSerialWrapper[] { wrapper }),
							logger);
			String msg = "OK ";
			logger.info(msg);
			item.setShortDescription(msg);
			JobRegistry.get().jobComplete(jobInfo);
		} catch (Exception ex) {
			ex.printStackTrace();
			item.setShortDescription("Failed");
			logger.warn(ex);
			JobRegistry.get().jobError(jobInfo, ex.toString());
		}
		long t2 = System.currentTimeMillis();
		long avgTime = (docsImported != 0) ? (t2 - t1) / docsImported : 0;
		logger.info(String.format(
				"Run time: %.4f s. - avg. time per doc: %s ms.",
				((float) (t2 - t1)) / 1000, avgTime));
		item.setActionLog(ServletLayerLocator.get()
				.remoteActionLoggerProvider().closeLogger(this.getClass()));
		return item;
	}

	public ActionLogItem performAction(DtrSimpleAdminPersistenceAction action) {
		return commit(action.getParameters());
	}
}
