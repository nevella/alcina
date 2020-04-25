package cc.alcina.framework.gwt.client.entity.place;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.entity.HasEntityAction;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class EntitySubPlace<E extends Enum, SD extends EntitySearchDefinition>
        extends EntityPlace<SD> implements ClearableIdPlace, HasEntityAction {
    public abstract E getSub();

    @Override
    public String toTitleString() {
        String category = super.toTitleString();
        Enum sub = getSub();
        if (sub != null) {
            category = HasDisplayName.displayName(sub);
        }
        if (id != 0) {
            BasePlaceTokenizer tokenizer = RegistryHistoryMapper.get()
                    .getTokenizer(this);
            Entity modelObject = TransformManager.get()
                    .getObject(tokenizer.getModelClass(), id, 0);
            if (modelObject == null
                    || !(modelObject instanceof HasDisplayName)) {
                return Ax.format("%s #%s", category, id);
            } else {
                String text = HasDisplayName.displayName(modelObject);
                return Ax.format("%s %s", category,
                        CommonUtils.trimToWsChars(text, 20, true));
            }
        }
        if (def != null) {
            if (def.provideIsSimpleTextSearch()) {
                String text = def.provideSimpleTextSearchCriterion().getValue();
                return Ax.format("%s '%s'", category,
                        CommonUtils.trimToWsChars(text, 20, true));
            }
        }
        return super.toTitleString();
    }
}