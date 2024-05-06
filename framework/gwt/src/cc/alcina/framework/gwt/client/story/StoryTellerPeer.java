package cc.alcina.framework.gwt.client.story;

import java.util.LinkedHashMap;
import java.util.Map;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObserver;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.gwt.client.story.Story.State;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit;

public class StoryTellerPeer implements TellerContext {
	class SimpleDependencyResolver implements DependencyResolver {
		@Override
		public Story.State.Provider
				resolveSatisfies(Class<? extends State> state) {
			return Registry.impl(State.Provider.class, state);
		}
	}

	class BeforeVisitObserver
			implements ProcessObserver<StoryTeller.BeforeVisit> {
		@Override
		public void topicPublished(StoryTeller.BeforeVisit beforeVisit) {
			Visit visit = beforeVisit.getVisit();
			if (visit.result.isFiltered()) {
				return;
			}
			visit.result.logEntry().template("> %s").args(visit.displayName())
					.types(StoryTeller.LogType.PROCESS).log();
		}
	}

	protected StoryTeller storyTeller;

	protected DependencyResolver dependencyResolver;

	Map<Class<? extends Part>, Part> parts = new LinkedHashMap<>();

	protected boolean throwOnFailure;

	public boolean isThrowOnFailure() {
		return throwOnFailure;
	}

	public void setThrowOnFailure(boolean throwOnFailure) {
		this.throwOnFailure = throwOnFailure;
	}

	@Override
	public void init(StoryTeller storyTeller) {
		this.storyTeller = storyTeller;
		initServices();
		initObservers();
	}

	@Override
	public Story.State.Provider resolveSatisfies(Class<? extends State> state) {
		return dependencyResolver.resolveSatisfies(state);
	}

	@Override
	public <P extends Part> P getPart(Class<? extends Part> clazz) {
		return (P) parts.computeIfAbsent(clazz, Reflections::newInstance);
	}

	protected void addPart(Class<? extends Part> clazz, Part part) {
		parts.put(clazz, part);
	}

	protected void initServices() {
		dependencyResolver = new SimpleDependencyResolver();
	}

	protected void initObservers() {
		ProcessObservers.context().observe(new BeforeVisitObserver());
	}
}
