package cc.alcina.framework.entity.entityaccess.cache;

import cc.alcina.framework.common.client.domain.DomainDescriptor;
import cc.alcina.framework.common.client.logic.domaintransform.ClassRef;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.entity.domaintransform.DomainTransformRequestPersistent;

public abstract class DomainStoreDescriptor extends DomainDescriptor {
    protected DomainSegmentLoader domainSegmentLoader;

    public DomainSegmentLoader getDomainSegmentLoader() {
        return domainSegmentLoader;
    }

    public abstract Class<? extends DomainTransformRequestPersistent> getDomainTransformRequestPersistentClass();

    public Class<? extends ClassRef> getShadowClassRefClass() {
        throw new UnsupportedOperationException();
    }

    public Class<? extends DomainTransformEvent> getShadowDomainTransformEventPersistentClass() {
        throw new UnsupportedOperationException();
    }

    public boolean isUseTransformDbCommitSequencing() {
        return true;
    }

    public void saveSegmentData() {
        throw new UnsupportedOperationException();
    }
}
