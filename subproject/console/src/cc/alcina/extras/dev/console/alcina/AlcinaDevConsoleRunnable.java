package cc.alcina.extras.dev.console.alcina;

import cc.alcina.extras.dev.console.DevConsoleRunnable;

public abstract class AlcinaDevConsoleRunnable extends DevConsoleRunnable {
	public AlcinaTag[] tags() {
		return new AlcinaTag[0];
	}

	@Override
	public boolean requiresDomainStore() {
		return false;
	}

	@Override
	public String[] tagStrings() {
		AlcinaTag[] tags = tags();
		String[] tagStrings = new String[tags.length];
		for (int i = 0; i < tags.length; i++) {
			AlcinaTag dcrTag = tags[i];
			tagStrings[i] = dcrTag.toString();
		}
		return tagStrings;
	}

	public enum AlcinaTag {
	}
}
