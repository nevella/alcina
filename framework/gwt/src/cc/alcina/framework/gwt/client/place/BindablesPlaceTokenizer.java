package cc.alcina.framework.gwt.client.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.gwt.client.entity.search.BindableSearchDefinition;

public abstract class BindablesPlaceTokenizer<HL extends Bindable, SD extends BindableSearchDefinition, P extends BindablePlace<SD>>
		extends BasePlaceTokenizer<P> {
	protected static final String P_DEF = "d";

	public abstract Class<HL> getModelClass();

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
		parseMap(detail);
		deserializeSearchDefinition(place);
		return place;
	}

	@Override
	protected void getToken0(P place) {
		if (place.provideIsDefaultDefs()) {
		} else {
			initOutParams();
			setParameter(P_DEF,
					searchDefinitionSerializer().serialize(place.def));
		}
	}
}