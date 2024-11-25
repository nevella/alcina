package cc.alcina.framework.gwt.client.dirndl.cmp.help;

import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.gwt.client.place.BasePlace;
import cc.alcina.framework.gwt.client.place.BasePlaceTokenizer;

public class HelpPlace extends BasePlace implements HasDisplayName {
	public static final transient String ROOT = ".";

	public String nodePath;

	public HelpPlace withNodePath(String nodePath) {
		this.nodePath = nodePath;
		return this;
	}

	@Override
	public String displayName() {
		throw new UnsupportedOperationException();
	}

	public static class Tokenizer extends BasePlaceTokenizer<HelpPlace> {
		@Override
		protected HelpPlace getPlace0(String token) {
			HelpPlace place = new HelpPlace();
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
		protected void getToken0(HelpPlace place) {
			if (place.nodePath != null) {
				addTokenPart(place.nodePath);
			}
		}
	}

	public static BasePlace toggleRoot(BasePlace place) {
		if (place.fragments().has(HelpPlace.class)) {
			place.fragments().remove(HelpPlace.class);
		} else {
			place.fragments().ensure(HelpPlace.class)
					.withNodePath(HelpPlace.ROOT);
		}
		return place;
	}
}
