package cc.alcina.framework.entity.util;

import cc.alcina.framework.gwt.client.util.LineCallback;

public class TabbedSysoutCallback implements LineCallback {
	private String prompt;

	public TabbedSysoutCallback(String prompt) {
		this.prompt = prompt;
	}

	@Override
	public void accept(String value) {
		System.out.print(prompt + value);
		if (!value.endsWith("\n")) {
			System.out.print('\n');
		}
	}
}