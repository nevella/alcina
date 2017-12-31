package cc.alcina.extras.ios6.client;

public class IosSplitNonIos6 implements IosSplit {
	@Override
	public String getMarker() {
		return "non-ios6";
	}
}
