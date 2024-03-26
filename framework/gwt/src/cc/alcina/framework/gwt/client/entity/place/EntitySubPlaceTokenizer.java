package cc.alcina.framework.gwt.client.entity.place;

import cc.alcina.framework.common.client.domain.search.EntitySearchDefinition;
import cc.alcina.framework.common.client.logic.domain.Entity;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.entity.EntityAction;

public abstract class EntitySubPlaceTokenizer<E extends Enum, ENT extends Entity, SD extends EntitySearchDefinition, P extends EntitySubPlace<E, SD>>
		extends EntityPlaceTokenizer<ENT, SD, P> {
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
		String part2 = parts[2 + offset];
		String part3 = 3 + offset < parts.length ? parts[3 + offset] : null;
		EntityAction entityAction = enumValue(EntityAction.class, part2);
		String detail = null;
		/*
		 * the url forms are
		 * 
		 * /<entity>/<id>/<action> (for id-specific actions)
		 * 
		 * or
		 * 
		 * /<entity>/<action>/<parameters>
		 * 
		 * or
		 * 
		 * /<entity>/<parameters> [default action - view]
		 */
		if (entityAction != null) {
			place.action = entityAction;
			if (entityAction == EntityAction.CREATE
					&& parts.length > 3 + offset) {
				place.fromId = Long.parseLong(parts[3 + offset]);
			}
			if (entityAction == EntityAction.CREATE
					&& parts.length > 4 + offset) {
				place.fromClass = parts[4 + offset];
			}
			if (entityAction == EntityAction.VIEW
					|| entityAction == EntityAction.EDIT) {
				detail = part3;
			}
		} else {
			if (part2.matches("[\\-0-9]+")) {
				place.id = Long.parseLong(part2);
				if (part3 != null) {
					place.action = enumValue(EntityAction.class, part3);
				}
			} else {
				detail = part2;
			}
		}
		if (detail != null) {
			parseMap(detail);
			deserializeSearchDefinition(place);
		}
		return place;
	}

	public E getSub() {
		return Reflections.at(getTokenizedClass()).templateInstance().getSub();
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

	@Override
	public Class<P> getTokenizedClass() {
		return Reflections.at(getClass()).getGenericBounds().bounds.get(3);
	}
}