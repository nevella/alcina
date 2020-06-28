package cc.alcina.framework.entity.entityaccess.cache;

import java.util.List;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.entity.SEUtilities;

public abstract class DomainModificationMetadataProvider {
	public DomainModificationMetadataProvider(DomainStore store) {
	}

	public abstract void
			registerTransforms(List<DomainTransformEvent> transforms);

	public abstract void updateMetadata(DomainTransformEvent dte,
			Entity domainStoreObject);

	protected void updateIVersionable(Entity obj,
			Object persistentLayerSource) {
		IVersionable graph = (IVersionable) obj;
		IVersionable persistent = (IVersionable) persistentLayerSource;
		graph.setCreationDate(
				SEUtilities.toJavaDate(persistent.getCreationDate()));
		graph.setLastModificationDate(
				SEUtilities.toJavaDate((persistent.getLastModificationDate())));
		return;
	}

	protected void updateVersionNumber(Entity obj,
			Entity persistentLayerSource) {
		((HasVersionNumber) obj).setVersionNumber(
				((HasVersionNumber) persistentLayerSource).getVersionNumber());
	}
}
