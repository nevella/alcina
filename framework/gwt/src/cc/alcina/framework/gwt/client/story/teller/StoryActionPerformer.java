package cc.alcina.framework.gwt.client.story.teller;

import cc.alcina.framework.gwt.client.story.teller.StoryTeller.Visit;

class StoryActionPerformer {
	class Result {
		boolean ok;
	}

	Result result = new Result();

	public Result perform(Visit visit) {
		return result;
	}
}
