package cc.alcina.framework.entity.entityaccess.cache;

import java.util.List;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domain.HiliHelper;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.permissions.IUser;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;
import cc.alcina.framework.entity.SEUtilities;

public abstract class DomainModificationMetadataProvider {
    private DomainStore store;

    public DomainModificationMetadataProvider(DomainStore store) {
        this.store = store;
    }

    public abstract void registerTransforms(
            List<DomainTransformEvent> transforms);

    public abstract void updateMetadata(DomainTransformEvent dte,
            HasIdAndLocalId domainStoreObject);

    protected void updateIVersionable(HasIdAndLocalId obj,
            Object persistentLayerSource) {
        IVersionable graph = (IVersionable) obj;
        IVersionable persistent = (IVersionable) persistentLayerSource;
        graph.setCreationDate(
                SEUtilities.toJavaDate(persistent.getCreationDate()));
        graph.setLastModificationDate(
                SEUtilities.toJavaDate((persistent.getLastModificationDate())));
        Class<? extends IUser> iUserClass = store.domainDescriptor
                .getIUserClass();
        if (iUserClass == null) {
            return;
        }
        Long persistentCreationUserId = HiliHelper
                .getIdOrNull(persistent.getCreationUser());
        IUser creationUser = store.cache.get(iUserClass,
                persistentCreationUserId);
        graph.setCreationUser(creationUser);
        Long persistentLastModificationUserId = HiliHelper
                .getIdOrNull(persistent.getLastModificationUser());
        IUser lastModificationUser = store.cache.get(iUserClass,
                persistentLastModificationUserId);
        graph.setLastModificationUser(lastModificationUser);
    }

    protected void updateVersionNumber(HasIdAndLocalId obj,
            DomainTransformEvent dte) {
        ((HasVersionNumber) obj).setVersionNumber(
                ((HasVersionNumber) dte.getSource()).getVersionNumber());
    }
}
