package cc.alcina.framework.servlet.actionhandlers;

import java.util.Arrays;

import org.apache.log4j.Logger;

import cc.alcina.framework.common.client.actions.ActionLogItem;
import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.csobjects.JobTracker;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.entity.SEUtilities;
import cc.alcina.framework.entity.entityaccess.CommonPersistenceProvider;
import cc.alcina.framework.gwt.client.action.DtrSimpleAdminPersistenceAction;
import cc.alcina.framework.servlet.CommonRemoteServletProvider;
import cc.alcina.framework.servlet.RemoteActionLoggerProvider;
import cc.alcina.framework.servlet.job.BaseRemoteActionPerformer;
import cc.alcina.framework.servlet.job.JobRegistry;

@RegistryLocation(registryPoint = RemoteActionPerformer.class, targetClass = DtrSimpleAdminPersistenceAction.class)
public class DtrSimpleAdminPersistenceHandler extends
		BaseRemoteActionPerformer<DtrSimpleAdminPersistenceAction> {
	public void commit(DeltaApplicationRecord wrapper) {
		try {
			jobStarted();
			String t = wrapper.getText();
			wrapper.setType(DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED);
			Registry.impl(CommonRemoteServletProvider.class)
					.getCommonRemoteServiceServlet()
					.persistOfflineTransforms(
							Arrays.asList(new DeltaApplicationRecord[] { wrapper }),
							logger);
			jobOk("OK");
		} catch (Exception ex) {
			jobError(ex);
		}
	}

	public void performAction(DtrSimpleAdminPersistenceAction action) {
		commit(action.getParameters());
	}
}
