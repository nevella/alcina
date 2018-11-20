package cc.alcina.framework.servlet.actionhandlers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.actions.RemoteActionPerformer;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecord;
import cc.alcina.framework.common.client.logic.domaintransform.DeltaApplicationRecordType;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformRequest;
import cc.alcina.framework.common.client.logic.domaintransform.TransformType;
import cc.alcina.framework.common.client.logic.reflection.RegistryLocation;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.util.IntPair;
import cc.alcina.framework.entity.ResourceUtilities;
import cc.alcina.framework.gwt.client.action.DtrSimpleAdminPersistenceAction;
import cc.alcina.framework.servlet.CommonRemoteServletProvider;
import cc.alcina.framework.servlet.job.BaseRemoteActionPerformer;

@RegistryLocation(registryPoint = RemoteActionPerformer.class, targetClass = DtrSimpleAdminPersistenceAction.class)
public class DtrSimpleAdminPersistenceHandler
		extends BaseRemoteActionPerformer<DtrSimpleAdminPersistenceAction> {
	public void commit(DeltaApplicationRecord dar) {
		int chunkSize = ResourceUtilities.getInteger(
				DtrSimpleAdminPersistenceHandler.class, "chunkSize", 5000);
		commit(dar, chunkSize);
	}

	public void commit(DeltaApplicationRecord dar, int chunkSize) {
		try {
			jobStarted();
			DomainTransformRequest fullRq = new DomainTransformRequest();
			fullRq.fromString(dar.getText());
			int size = fullRq.getEvents().size();
			if (size > chunkSize) {
				getJobTracker().setItemCount(size / chunkSize + 1);
				int rqIdCounter = dar.getRequestId();
				for (int idx = 0; idx < size;) {
					DeltaApplicationRecord chunk = new DeltaApplicationRecord(0,
							"", dar.getTimestamp(), dar.getUserId(),
							dar.getClientInstanceId(), rqIdCounter++,
							dar.getClientInstanceAuth(),
							DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED,
							dar.getProtocolVersion(), dar.getTag());
					DomainTransformRequest rq = new DomainTransformRequest();
					IntPair range = null;
					if (idx + chunkSize > size) {
						range = new IntPair(idx, size);
					} else {
						int createSearchIdx = idx + chunkSize;
						int maxCreateIdxDelta = size / 2;
						for (; createSearchIdx < size
								&& maxCreateIdxDelta > 0;) {
							DomainTransformEvent evt = fullRq.getEvents()
									.get(createSearchIdx);
							if (evt.getTransformType() == TransformType.CREATE_OBJECT
									|| evt.getTransformType() == TransformType.DELETE_OBJECT) {
								range = new IntPair(idx, createSearchIdx);
								break;
							}
							createSearchIdx++;
							maxCreateIdxDelta--;
						}
						if (range == null) {
							range = new IntPair(idx, idx + chunkSize);
						}
					}
					List<DomainTransformEvent> subList = fullRq.getEvents()
							.subList(range.i1, range.i2);
					rq.setRequestId(chunk.getRequestId());
					rq.setEvents(new ArrayList<DomainTransformEvent>(subList));
					chunk.setText(rq.toString());
					Registry.impl(CommonRemoteServletProvider.class)
							.getCommonRemoteServiceServlet()
							.persistOfflineTransforms(Arrays.asList(
									new DeltaApplicationRecord[] { chunk }),
									logger, false, true);
					String message = String.format(
							"written chunk - writing chunk %s of %s", range,
							size);
					System.out.println(message);
					updateJob(message);
					idx = range.i2;
				}
			} else {
				dar.setType(
						DeltaApplicationRecordType.LOCAL_TRANSFORMS_APPLIED);
				Registry.impl(CommonRemoteServletProvider.class)
						.getCommonRemoteServiceServlet()
						.persistOfflineTransforms(
								Arrays.asList(
										new DeltaApplicationRecord[] { dar }),
								logger);
			}
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
