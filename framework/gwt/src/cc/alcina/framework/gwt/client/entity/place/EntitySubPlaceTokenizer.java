package cc.alcina.framework.gwt.client.entity.place;

import cc.alcina.framework.common.client.Reflections;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.entity.search.EntitySearchDefinition;

public abstract class EntitySubPlaceTokenizer<E extends Enum, ENT extends Entity, SD extends EntitySearchDefinition, P extends EntitySubPlace<E, SD>>
		extends EntityPlaceTokenizer<ENT, SD, P> {
	public E getSub() {
		return Reflections.classLookup()
				.getTemplateInstance(getTokenizedClass()).getSub();
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
		EntityAction listAction = enumValue(EntityAction.class, detail);
		if (listAction != null) {
			place.action = listAction;
			if (listAction == EntityAction.CREATE
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
				place.action = enumValue(EntityAction.class, action);
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
		if (place.action != null && place.action != EntityAction.VIEW) {
			addTokenPart(place.action.toString().toLowerCase());
		}
	}
}