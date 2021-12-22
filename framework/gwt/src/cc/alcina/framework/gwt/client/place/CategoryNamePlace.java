package cc.alcina.framework.gwt.client.place;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import cc.alcina.framework.common.client.actions.PermissibleAction;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.HasDisplayName;

public abstract class CategoryNamePlace<CNP extends CategoryNamePlace>
		extends BasePlace implements HasDisplayName {
	protected static <CNP extends CategoryNamePlace> CNP
			namedPlaceForAction(Class<CNP> clazz, PermissibleAction action) {
		CNP place = Reflections.newInstance(clazz);
		place.action = action;
		place.nodeName = action.provideId();
		return place;
	}

	public String nodeName;

	public String provideCategoryString() {
		return CommonUtils.pluralise(super.toTitleString(), 0, false);
	}

	protected transient PermissibleAction action;

	public abstract List<CNP> getNamedPlaces();

	public PermissibleAction ensureAction() {
		if (action == null) {
			action = getNamedPlaces().stream()
					.filter(place -> Objects.equals(place.nodeName, nodeName))
					.findFirst().map(cnp -> cnp.action).orElse(null);
		}
		return action;
	}

	@Override
	public String displayName() {
		return ensureAction().getDisplayName();
	}

	@Override
	public String toTitleString() {
		if (nodeName == null) {
			return provideCategoryString();
		} else {
			return Ax.format("%s - %s", provideCategoryString(), nodeName);
		}
	}

	@Override
	public String toNameString() {
		if (nodeName == null) {
			return toTitleString();
		} else {
			return displayName();
		}
	}

	protected List<CNP> getNamedPlaces(Class targetClass) {
		return (List) Registry.impls(PermissibleAction.class, targetClass)
				.stream().sorted(Comparator.comparing(a -> a.provideId()))
				.map(action -> namedPlaceForAction(getClass(), action))
				.collect(Collectors.toList());
	}

	public static abstract class CategoryNamePlaceTokenizer<CNP extends CategoryNamePlace>
			extends BasePlaceTokenizer<CNP> {
		@Override
		public abstract Class<CNP> getTokenizedClass();

		@Override
		protected CNP getPlace0(String token) {
			CNP place = Reflections.newInstance(getTokenizedClass());
			if (parts.length > 1) {
				place.nodeName = parts[1];
			}
			return place;
		}

		@Override
		protected void getToken0(CategoryNamePlace place) {
			if (place.nodeName != null) {
				addTokenPart(place.nodeName);
			}
		}
	}
}
