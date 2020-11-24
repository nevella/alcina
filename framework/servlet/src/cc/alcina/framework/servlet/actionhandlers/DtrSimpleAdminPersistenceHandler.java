package cc.alcina.framework.servlet.actionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.actions.TaskPerformer;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.gwt.client.action.DtrSimpleAdminPersistenceAction;
import cc.alcina.framework.servlet.job.BaseRemoteActionPerformer;

@RegistryLocation(registryPoint = TaskPerformer.class, targetClass = DtrSimpleAdminPersistenceAction.class)
public class DtrSimpleAdminPersistenceHandler
		extends BaseRemoteActionPerformer<DtrSimpleAdminPersistenceAction> {
	Logger slf4jLogger = LoggerFactory.getLogger(getClass());

	public void commit(DeltaApplicationRecord dar) {
		try {
			int chunkSize = ResourceUtilities.getInteger(
					DtrSimpleAdminPersistenceHandler.class, "chunkSize", 5000);
			TransformCommit.commitDeltaApplicationRecord(dar, chunkSize);
			jobOk("OK");
		} catch (Exception ex) {
			jobError(ex);
		}
	}

	@Override
	public void performAction(DtrSimpleAdminPersistenceAction action) {
		commit(action.getParameters());
	}
}
