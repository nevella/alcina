package cc.alcina.framework.gwt.client.dirndl.cmp.told;

import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

public class ToldPlace extends BasePlace implements HasDisplayName {
	public static final transient String ROOT = ".";

	public String nodePath;

	public ToldPlace withNodePath(String nodePath) {
		this.nodePath = nodePath;
		return this;
	}

	@Override
	public String displayName() {
		throw new UnsupportedOperationException();
	}

	public static class Tokenizer extends BasePlaceTokenizer<ToldPlace> {
		@Override
		protected ToldPlace getPlace0(String token) {
			ToldPlace place = new ToldPlace();
			if (parts.length > 1) {
				place.nodePath = parts[1];
			}
			return place;
		}

		@Override
		public String getPrefix() {
			return "help";
		}

		@Override
		protected void getToken0(ToldPlace place) {
			if (place.nodePath != null) {
				addTokenPart(place.nodePath);
			}
		}
	}
}
