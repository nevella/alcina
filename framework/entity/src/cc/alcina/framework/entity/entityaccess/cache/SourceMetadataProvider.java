package cc.alcina.framework.entity.entityaccess.cache;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domain.HasVersionNumber;
import cc.alcina.framework.common.client.logic.domaintransform.DomainTransformEvent;
import cc.alcina.framework.common.client.logic.domaintransform.HiliLocator;
import cc.alcina.framework.common.client.logic.permissions.IVersionable;

class SourceMetadataProvider extends DomainModificationMetadataProvider {
    Map<HiliLocator, HasIdAndLocalId> locatorOriginalSourceMap = new LinkedHashMap<HiliLocator, HasIdAndLocalId>();

    public SourceMetadataProvider(DomainStore store) {
        super(store);
    }

    @Override
    public void registerTransforms(List<DomainTransformEvent> transforms) {
        for (DomainTransformEvent dte : transforms) {
            HiliLocator locator = HiliLocator.objectLocator(dte);
            locatorOriginalSourceMap.put(locator, dte.getSource());
        }
    }

    @Override
    public void updateMetadata(DomainTransformEvent dte,
            HasIdAndLocalId domainStoreObject) {
        // dte source is clobbered by early precache/preload - this'll probably
        // go away (why does preload need source?) but using this model for now
        HasIdAndLocalId dbObj = locatorOriginalSourceMap
                .get(HiliLocator.objectLocator(dte));
        if (domainStoreObject instanceof HasVersionNumber) {
            updateVersionNumber(domainStoreObject, dte);
        }
        if (domainStoreObject instanceof IVersionable) {
            updateIVersionable(domainStoreObject, dbObj);
        }
    }
}
