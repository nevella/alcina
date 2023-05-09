package cc.alcina.framework.servlet.actionhandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.entity.persistence.transform.TransformCommit;
import cc.alcina.framework.gwt.client.action.DtrSimpleAdminPersistenceAction;
import cc.alcina.framework.servlet.job.BaseRemoteActionPerformer;

public class DtrSimpleAdminPersistenceHandler
		extends BaseRemoteActionPerformer<DtrSimpleAdminPersistenceAction> {
	Logger slf4jLogger = LoggerFactory.getLogger(getClass());

	public void commit(DeltaApplicationRecord dar) {
		try {
			int chunkSize = Configuration.getInt("chunkSize");
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
