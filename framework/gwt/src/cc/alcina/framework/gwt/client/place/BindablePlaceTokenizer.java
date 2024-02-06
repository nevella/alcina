package cc.alcina.framework.gwt.client.place;

import java.util.Arrays;
import java.util.List;

import cc.alcina.framework.common.client.csobjects.Bindable;
import cc.alcina.framework.common.client.domain.search.BindableSearchDefinition;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.search.ReflectiveSearchDefinitionSerializer;
import cc.alcina.framework.gwt.client.logic.AlcinaHistory;

public abstract class BindablePlaceTokenizer<HL extends Bindable, SD extends BindableSearchDefinition, P extends BindablePlace<SD>>
		extends BasePlaceTokenizer<P> {
	protected static final String P_DEF = "d";

	protected SD deserializeDef(P place) {
		SD def = searchDefinitionSerializer().deserialize(place.def.getClass(),
				getStringParameter(P_DEF));
		if (def == null) {
			def = (SD) Reflections.newInstance(place.def.getClass());
		}
		return def;
	}

	protected void deserializeSearchDefinition(P place) {
		place.def = deserializeDef(place);
	}

	@Override
	protected List<String> encodedValues() {
		return Arrays.asList(P_DEF);
	}

	public abstract Class<HL> getModelClass();

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
			setParameter(P_DEF,
					searchDefinitionSerializer().serialize(place.def));
		}
	}

	@Override
	public Class<P> getTokenizedClass() {
		return Reflections.at(getClass()).getGenericBounds().bounds.get(2);
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