package cc.alcina.framework.gwt.client.story.teller;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.State;

public class StoryTellerPeer implements TellerContext {
	protected StoryTeller storyTeller;

	protected DependencyResolver dependencyResolver;

	@Override
	public void init(StoryTeller storyTeller) {
		this.storyTeller = storyTeller;
		initServices();
		initObservers();
	}

	protected void initServices() {
		dependencyResolver = new SimpleDependencyResolver();
	}

	class SimpleDependencyResolver implements DependencyResolver {
		@Override
		public Story.State.Provider
				resolveSatisfies(Class<? extends State> state) {
			return Registry.impl(State.Provider.class, state);
		}
	}

	protected void initObservers() {
		ProcessObservers.context().observe(new BeforeVisitObserver());
	}

	class BeforeVisitObserver
			implements ProcessObserver<StoryTeller.BeforeVisit> {
		@Override
		public void topicPublished(StoryTeller.BeforeVisit beforeVisit) {
			beforeVisit.getVisit().result.logEntry().template("> %s")
					.args(beforeVisit.getVisit().displayName()).log();
		}
	}

	@Override
	public Story.State.Provider resolveSatisfies(Class<? extends State> state) {
		return dependencyResolver.resolveSatisfies(state);
	}
}
