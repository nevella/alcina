package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.common.client.util.Callback;

/*
 * Logging stream callback. Here to keep the Story api gwt-compatible
 */
public interface LineCallback extends Callback<String> {
	public static class Noop implements LineCallback {
		@Override
		public void accept(String s) {
			//
		}
	}
}