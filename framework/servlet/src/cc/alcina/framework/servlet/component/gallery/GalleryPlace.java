package cc.alcina.framework.servlet.component.gallery;

import cc.alcina.framework.common.client.logic.reflection.reachability.Bean;
import cc.alcina.framework.common.client.logic.reflection.reachability.Bean.PropertySource;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.serializer.FlatTreeSerializer;
import cc.alcina.framework.common.client.serializer.TreeSerializable;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

/**
 * 
 * 
 */
@Bean(PropertySource.FIELDS)
public abstract class GalleryPlace extends BasePlace
		implements GalleryBrowserPlace, TreeSerializable {
	@Override
	public GalleryPlace copy() {
		return super.copy();
	}

	public String toNameString() {
		return CommonUtils
				.deInfix(toTitleString().replaceFirst("Gallery$", ""));
	}

	public abstract String getDescription();

	public abstract static class Tokenizer<RP extends GalleryPlace>
			extends BasePlaceTokenizer<RP> {
		@Override
		public String getPrefix() {
			return super.getPrefix().replaceFirst("^gallery", "");
		}

		@Override
		protected RP getPlace0(String token) {
			Class<? extends GalleryPlace> type = Reflections.at(getClass())
					.firstGenericBound();
			GalleryPlace place = Reflections.newInstance(type);
			if (parts.length > 1) {
				try {
					place = FlatTreeSerializer.deserialize(type, parts[1]);
				} catch (Exception e) {
					Ax.simpleExceptionOut(e);
				}
			}
			return (RP) place;
		}

		@Override
		protected void getToken0(RP place) {
			addTokenPart(FlatTreeSerializer.serializeSingleLine(place));
		}
	}
}
