package cc.alcina.framework.entity.util;

import cc.alcina.framework.common.client.util.Callback;

public class TabbedSysoutCallback implements Callback<String> {
	private String prompt;

	@Override
	public void apply(String value) {
		System.out.println(prompt + value);
	}

	public TabbedSysoutCallback(String prompt) {
		this.prompt = prompt;
	}
}