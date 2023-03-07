package cc.alcina.framework.gwt.client.entity.place;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

/**
 * This is a legacy-only class - since it's better to strongly model the
 * parameters (otherwise there's extra code handler side, and less type-safety)
 */
@Registration(ActionPlace.class)
public abstract class ActionPlace extends BasePlace {
	public List<String> parameters = new ArrayList<>();

	public ActionPlace() {
	}

	public void addParameter(Enum e) {
		addParameter(e.toString().toLowerCase());
	}

	public void addParameter(Long longValue) {
		if (longValue == null) {
			parameters.add("0");
		} else {
			parameters.add(longValue.toString());
		}
	}

	public void addParameter(String string) {
		if (string == null) {
			string = "";
		}
		parameters.add(string);
	}

	public abstract String getName();

	public long longParameter(int idx) {
		return Long.parseLong(parameters.get(idx));
	}

	public static class Tokenizer extends BasePlaceTokenizer<ActionPlace> {
		Map<String, ClassReflector<? extends ActionPlace>> byName;

		@Override
		public Class<ActionPlace> getTokenizedClass() {
			return ActionPlace.class;
		}

		private void ensureLookup() {
			if (byName == null) {
				Map<String, ClassReflector<? extends ActionPlace>> byName = AlcinaCollections
						.newHashMap();
				Registry.query(ActionPlace.class).registrations()
						.map(Reflections::at).forEach(refl -> {
							ActionPlace instance = refl.templateInstance();
							String key = instance.getName();
							if (byName.containsKey(key)) {
								throw new IllegalArgumentException(Ax.format(
										"Identical keys - '%s' - [%s,%s]", key,
										byName.get(key), refl));
							}
							byName.put(key, refl);
						});
				this.byName = byName;
			}
		}

		@Override
		protected ActionPlace getPlace0(String token) {
			if (parts.length < 2) {
				return null;
			}
			String actionName = parts[1];
			ClassReflector<? extends ActionPlace> reflector = getReflector(
					actionName);
			if (reflector == null) {
				return null;
			}
			ActionPlace place = reflector.newInstance();
			for (int i = 2; i < parts.length; i++) {
				place.parameters.add(parts[i]);
			}
			return place;
		}

		@Override
		protected void getToken0(ActionPlace place) {
			addTokenPart(place.getName());
			for (String parameter : place.parameters) {
				addTokenPart(parameter);
			}
		}

		@Override
		protected boolean handlesPlaceSubclasses() {
			return true;
		}

		ClassReflector<? extends ActionPlace> getReflector(String actionName) {
			ensureLookup();
			ClassReflector<? extends ActionPlace> reflector = byName
					.get(actionName);
			if (reflector == null) {
				byName = null;
				ensureLookup();
			}
			return reflector;
		}
	}
}
