package cc.alcina.framework.gwt.client.place;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.search.ReflectiveSearchDefinitionSerializer;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

public abstract class BindablePlaceTokenizer<HL extends Bindable, SD extends BindableSearchDefinition, P extends BindablePlace<SD>>
		extends BasePlaceTokenizer<P> {
	protected static final String P_DEF = "d";

	public abstract Class<HL> getModelClass();

	protected void deserializeSearchDefinition(P place) {
		place.def = searchDefinitionSerializer()
				.deserialize(place.def.getClass(), getStringParameter(P_DEF));
	}

	@Override
	protected List<String> encodedValues() {
		return Arrays.asList(P_DEF);
	}

	@Override
	protected P getPlace0(String token) {
		P place = Reflections.newInstance(getTokenizedClass());
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

	@Override
	protected void parseMap(String s) {
		params = AlcinaHistory.fromHash(s, (k, v) -> {
			if (k.equals(P_DEF) && !v
					.startsWith(ReflectiveSearchDefinitionSerializer.RS0)) {
				return true;
			}
			return false;
		});
	}
}