package cc.alcina.framework.gwt.client.data.place;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlTransient;

import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.common.client.logic.domaintransform.TransformManager;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.data.DataAction;
import cc.alcina.framework.gwt.client.data.HasDataAction;
import cc.alcina.framework.gwt.client.data.search.DataSearchDefinition;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.gwt.client.place.GenericBasePlace;
import cc.alcina.framework.gwt.client.place.RegistryHistoryMapper;

@XmlAccessorType(XmlAccessType.FIELD)
public abstract class DataPlace<SD extends DataSearchDefinition> extends
		GenericBasePlace<SD> implements ClearableIdPlace, HasDataAction {
	public DataAction action;

	@Override
	public void clearIds() {
		id = 0;
	}

	@Override
	@XmlTransient
	public DataAction getAction() {
		return this.action;
	}

	@Override
	public SD getSearchDefinition() {
		return super.getSearchDefinition();
	}

	@Override
	public boolean provideIsDefaultDefs() {
		return def.provideHasNoCriteria()
				&& def.getGroupingParameters() == null;
	}

	@Override
	public void setAction(DataAction action) {
		this.action = action;
	}

	@Override
	public String toTitleString() {
		String category = super.toTitleString();
		if (id != 0) {
			BasePlaceTokenizer tokenizer = RegistryHistoryMapper.get()
					.getTokenizer(this);
			HasIdAndLocalId modelObject = TransformManager.get()
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