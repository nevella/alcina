package cc.alcina.framework.servlet.component.featuretree;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

public class FeaturePlace extends BasePlace {
	public Class<? extends Feature> feature;

	public static class Tokenizer extends BasePlaceTokenizer<FeaturePlace> {
		@Override
		protected FeaturePlace getPlace0(String token) {
			FeaturePlace place = new FeaturePlace();
			if (parts.length > 1) {
				try {
					place.feature = Reflections.forName(parts[1]);
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
			}
			return place;
		}

		@Override
		protected void getToken0(FeaturePlace place) {
			if (place.feature != null) {
				addTokenPart(place.feature.getName());
			}
		}
	}
}
