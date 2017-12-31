package cc.alcina.framework.gwt.client.place;

import com.google.gwt.place.shared.Place;
import com.google.gwt.regexp.shared.RegExp;

public abstract class BasePlaceRegexTokenizer<P extends Place>
		extends BasePlaceTokenizer<P> {
	@Override
	public abstract P getPlace(String token);

	@Override
	public abstract String getPrefix();

	@Override
	public abstract String getToken(P place);

	@Override
	public boolean handles(String token) {
		return getRegExpInstance().test(token);
	}

	@Override
	protected P getPlace0(String token) {
		throw new UnsupportedOperationException();
	}

	protected abstract RegExp getRegExpInstance();

	@Override
	protected void getToken0(P place) {
		throw new UnsupportedOperationException();
	}
}
