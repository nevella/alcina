package cc.alcina.framework.gwt.client.entity.place;

import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;
import cc.alcina.framework.gwt.client.place.OmniPlace;

public class KnownPlace extends BasePlace implements OmniPlace {
	public String nodePath;

	public KnownNavigationType navigationType = KnownNavigationType.List;

	@Override
	public String getOmniString() {
		return "Show knowns (cluster facts)";
	}

	public enum KnownNavigationType {
		List, Tree
	}

	public static class KnownPlaceTokenizer
			extends BasePlaceTokenizer<KnownPlace> {
		@Override
		protected KnownPlace getPlace0(String token) {
			KnownPlace place = new KnownPlace();
			if (parts.length > 1) {
				place.nodePath = parts[1];
			}
			if (parts.length > 2) {
				place.navigationType = enumValue(KnownNavigationType.class,
						parts[2]);
			}
			return place;
		}

		@Override
		protected void getToken0(KnownPlace place) {
			if (place.nodePath != null) {
				addTokenPart(place.nodePath);
			}
			if (place.navigationType != KnownNavigationType.List) {
				addTokenPart(place.navigationType);
			}
		}
	}
}
