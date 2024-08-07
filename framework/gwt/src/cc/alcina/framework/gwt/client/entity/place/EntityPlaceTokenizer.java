package cc.alcina.framework.gwt.client.entity.place;

import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.entity.EntityAction;
import cc.alcina.framework.gwt.client.place.BindablePlaceTokenizer;

public abstract class EntityPlaceTokenizer<E extends Entity, SD extends EntitySearchDefinition, P extends EntityPlace<SD>>
		extends BindablePlaceTokenizer<E, SD, P> {
	protected static final String P_DETAIL_ACTION = "a";

	@Override
	public abstract Class<E> getModelClass();

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
		EntityAction listAction = enumValue(EntityAction.class, detail);
		if (listAction != null) {
			place.action = listAction;
			if (listAction == EntityAction.CREATE
					&& parts.length > 3 + offset) {
				place.fromId = Long.parseLong(parts[3 + offset]);
			}
			if (listAction == EntityAction.CREATE
					&& parts.length > 4 + offset) {
				place.fromClass = parts[4 + offset];
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
				EntityAction actionEnum = enumValue(EntityAction.class, action);
				if (actionEnum != null) {
					place.action = actionEnum;
				}
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
			setParameter(P_DEF,
					searchDefinitionSerializer().serialize(place.def));
		}
		if (place.action != null && place.action != EntityAction.VIEW) {
			addTokenPart(place.action.toString().toLowerCase());
		}
		if (place.fromId != 0) {
			addTokenPart(place.fromId);
		}
		if (place.fromClass != null) {
			addTokenPart(place.fromClass);
		}
	}
}