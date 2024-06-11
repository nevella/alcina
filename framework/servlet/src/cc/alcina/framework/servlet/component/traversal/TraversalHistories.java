package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.traversal.Layer;
import cc.alcina.framework.common.client.traversal.SelectionTraversal;
import cc.alcina.framework.common.client.traversal.TraversalContext;
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
public class TraversalHistories extends LifecycleService.AlsoDev {
	public static TraversalHistories get() {
		return Registry.impl(TraversalHistories.class);
	}

	public RemoteComponentObservables<SelectionTraversal> observables;

	public TraversalHistories() {
		observables = new RemoteComponentObservables<>(
				TraversalProcessView.Component.class, SelectionTraversal.class,
				t -> {
					Layer rootLayer = t.getRootLayer();
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
		if (traversal
				.context(TraversalDoesNotPublishNullObservable.class) == null) {
			observables.publish(null, traversal);
		}
		observables.publish(traversal.id, traversal);
	}

	public ListenerReference subscribe(String traversalKey,
			TopicListener<RemoteComponentObservables<SelectionTraversal>.ObservableHistory> subscriber) {
		return observables.subscribe(traversalKey, subscriber);
	}

	/*
	 * Marker, if the TraversalContext implements this then a null-topic
	 * completion obsevable will not be published
	 */
	public interface TraversalDoesNotPublishNullObservable
			extends TraversalContext {
	}

	public void evict(SelectionTraversal traversal) {
		observables.evict(traversal.id);
	}
}
