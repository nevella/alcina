package cc.alcina.framework.gwt.client.entity.place;

import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.ClassReflector;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

/*
 */
@Registration(NamedPlace.class)
public abstract class NamedPlace extends BasePlace {
	public NamedPlace() {
	}

	public String getName() {
		return getClass().getSimpleName().replaceFirst("Place$", "");
	}

	@Override
	public String toTitleString() {
		return getName();
	}

	public static class Tokenizer extends BasePlaceTokenizer<NamedPlace> {
		Map<String, ClassReflector<? extends NamedPlace>> byName;

		private void ensureLookup() {
			if (byName == null) {
				Map<String, ClassReflector<? extends NamedPlace>> byName = AlcinaCollections
						.newHashMap();
				Registry.query(NamedPlace.class).registrations()
						.map(Reflections::at).forEach(refl -> {
							NamedPlace instance = refl.templateInstance();
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
		protected NamedPlace getPlace0(String token) {
			ClassReflector<? extends NamedPlace> reflector = getReflector(
					token);
			if (reflector == null) {
				return null;
			}
			NamedPlace place = reflector.newInstance();
			return place;
		}

		@Override
		public String getPrefix() {
			return "";
		}

		ClassReflector<? extends NamedPlace> getReflector(String actionName) {
			ensureLookup();
			ClassReflector<? extends NamedPlace> reflector = byName
					.get(actionName);
			if (reflector == null) {
				byName = null;
				ensureLookup();
			}
			return reflector;
		}

		@Override
		protected void getToken0(NamedPlace place) {
			addTokenPart(place.getName());
		}

		@Override
		public boolean handles(String token) {
			ensureLookup();
			return byName.containsKey(token);
		}

		@Override
		protected boolean handlesPlaceSubclasses() {
			return true;
		}
	}
}
