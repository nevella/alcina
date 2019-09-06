package cc.alcina.framework.gwt.client.data.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.HasIdAndLocalId;
import cc.alcina.framework.gwt.client.data.DataAction;
import cc.alcina.framework.gwt.client.data.search.DataSearchDefinition;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

public abstract class DataSubPlaceTokenizer<E extends Enum, HL extends HasIdAndLocalId, SD extends DataSearchDefinition, P extends DataSubPlace<E, SD>>
		extends BasePlaceTokenizer<P> {
	protected static final String P_DEF = "d";

	protected static final String P_DETAIL_ACTION = "a";

	@Override
	public abstract Class<HL> getModelClass();

	public E getSub() {
		return Reflections.classLookup()
				.getTemplateInstance(getTokenizedClass()).getSub();
	}

	protected void deserializeSearchDefinition(P place) {
		place.def = searchDefinitionSerializer()
				.deserialize(getStringParameter(P_DEF));
	}

	@Override
	protected P getPlace0(String token) {
		P place = Reflections.classLookup().newInstance(getTokenizedClass());
		int offset = 0;
		if (!getPrefix().contains("/")) {
			offset = -1;
		}
		if (parts.length < 3 + offset) {
			return place;
		}
		String detail = parts[2 + offset];
		DataAction listAction = enumValue(DataAction.class, detail);
		if (listAction != null) {
			place.action = listAction;
			if (listAction == DataAction.CREATE
					&& parts.length > 3 + offset) {
				place.fromId = Long.parseLong(parts[3 + offset]);
			}
		} else {
			if (detail.matches("[\\-0-9]+")) {
				place.id = Long.parseLong(detail);
			} else {
				parseMap(detail);
				deserializeSearchDefinition(place);
			}
			if (parts.length >= 4 + offset) {
				String action = parts[3 + offset];
				place.action = enumValue(DataAction.class, action);
			}
		}
		return place;
	}

	@Override
	protected void getToken0(P place) {
		if (place.provideIsDefaultDefs()) {
			if (place.id != 0) {
				addTokenPart(place.id);
			}
		} else {
			initOutParams();
			setParameter(P_DEF,
					searchDefinitionSerializer().serialize(place.def));
		}
		if (place.action != null && place.action != DataAction.VIEW) {
			addTokenPart(place.action.toString().toLowerCase());
		}
	}
}