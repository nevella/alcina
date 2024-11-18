package cc.alcina.framework.gwt.client.place;

import com.google.gwt.regexp.shared.RegExp;

public abstract class BasePlaceRegexTokenizer<P extends BasePlace>
		extends BasePlaceTokenizer<P> {
	@Override
	public abstract P getPlace(String token);

	@Override
	protected P getPlace0(String token) {
		throw new UnsupportedOperationException();
	}

	@Override
	public abstract String getPrefix();

	protected abstract RegExp getRegExpInstance();

	@Override
	public abstract String getToken(P place);

	@Override
	protected void getToken0(P place) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean handles(String token) {
		return getRegExpInstance().test(token);
	}
}
