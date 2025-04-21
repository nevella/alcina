package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
import cc.alcina.framework.common.client.traversal.TraversalContext.ShortTraversal;
import cc.alcina.framework.common.client.util.ListenerReference;
import cc.alcina.framework.common.client.util.NestedName;
import cc.alcina.framework.common.client.util.TimeConstants;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.entity.Configuration;
import cc.alcina.framework.servlet.LifecycleService;
import cc.alcina.framework.servlet.component.romcom.server.RemoteComponentObservables;

/**
 * <p>
 * This class observes and retains references to traversals
 * 
 */
@Registration.Singleton
public class TraversalObserver extends LifecycleService.AlsoDev {
	public static TraversalObserver get() {
		return Registry.impl(TraversalObserver.class);
	}

	public RemoteComponentObservables<SelectionTraversal> observables;

	public TraversalObserver() {
		observables = new RemoteComponentObservables<>(
				TraversalBrowser.Component.class, SelectionTraversal.class,
				t -> {
					Layer rootLayer = t.layers().getRoot();
					return Registry
							.impl(RootLayerNamer.class, rootLayer.getClass())
							.rootLayerName(rootLayer);
				}, Configuration.getInt("evictionMinutes")
						* TimeConstants.ONE_MINUTE_MS);
	}

	@Registration(RootLayerNamer.class)
	public static class RootLayerNamer<RL extends Layer> {
		public String rootLayerName(RL layer) {
			return NestedName.get(layer);
		}
	}

	public void observe() {
		SelectionTraversal.topicTraversalComplete
				.add(this::onTraversalComplete);
		observables.observe();
	}

	@Override
	public void onApplicationStartup() {
		if (Configuration.is("enabled")) {
			observe();
		}
	}

	void onTraversalComplete(SelectionTraversal traversal) {
		ShortTraversal shortTraversal = traversal
				.context(TraversalContext.ShortTraversal.class);
		if (shortTraversal == null || shortTraversal.provideRetain()) {
			observables.publish(null, traversal);
			observables.publish(traversal.id, traversal);
		}
	}

	public ListenerReference subscribe(String traversalKey,
			TopicListener<RemoteComponentObservables<SelectionTraversal>.ObservableEntry> subscriber) {
		return observables.subscribe(traversalKey, subscriber);
	}

	public void evict(SelectionTraversal traversal) {
		observables.evict(traversal.id);
	}

	void observableObserved(SelectionTraversal traversal) {
		observables.observed(traversal.id);
	}
}
