package cc.alcina.framework.gwt.client.story;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.story.Story.State;

public class StoryTellerPeer implements TellerContext {
	protected StoryTeller storyTeller;

	protected DependencyResolver dependencyResolver;

	Map<Class<? extends Part>, Part> parts = new LinkedHashMap<>();

	@Override
	public void init(StoryTeller storyTeller) {
		this.storyTeller = storyTeller;
		initServices();
		initObservers();
	}

	protected void addPart(Class<? extends Part> clazz, Part part) {
		parts.put(clazz, part);
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
					.args(beforeVisit.getVisit().displayName())
					.types(StoryTeller.LogType.PROCESS).log();
		}
	}

	@Override
	public Story.State.Provider resolveSatisfies(Class<? extends State> state) {
		return dependencyResolver.resolveSatisfies(state);
	}

	@Override
	public <P extends Part> P getPart(Class<? extends Part> clazz) {
		return (P) parts.computeIfAbsent(clazz, Reflections::newInstance);
	}
}
