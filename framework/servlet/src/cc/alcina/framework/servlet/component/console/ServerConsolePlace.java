package cc.alcina.framework.servlet.component.console;

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
public abstract class ServerConsolePlace extends BasePlace
		implements ServerConsoleBrowserPlace, TreeSerializable {
	@Override
	public ServerConsolePlace copy() {
		return super.copy();
	}

	public String toNameString() {
		return CommonUtils.deInfix(toTitleString());
	}

	public abstract String getDescription();

	public abstract static class Tokenizer<RP extends ServerConsolePlace>
			extends BasePlaceTokenizer<RP> {
		@Override
		public String getPrefix() {
			return super.getPrefix().replaceFirst("^serverconsole", "");
		}

		@Override
		protected RP getPlace0(String token) {
			Class<? extends ServerConsolePlace> type = Reflections
					.at(getClass()).firstGenericBound();
			ServerConsolePlace place = Reflections.newInstance(type);
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
