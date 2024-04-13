package cc.alcina.framework.gwt.client.story.teller;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.process.TreeProcess.HasProcessNode;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.story.Story;
import cc.alcina.framework.gwt.client.story.Story.Point;
import cc.alcina.framework.gwt.client.story.Story.State.Provider;
import cc.alcina.framework.gwt.client.story.teller.StoryActionPerformer.Result;

/**
 */
public class StoryTeller {
	public abstract class StoryTellerObservable
			implements ProcessObservers.ContextObservers.Observable {
		public Visit getVisit() {
			return state.current();
		}
	}

	public class BeforeStory extends StoryTellerObservable {
	}

	public class BeforeVisit extends StoryTellerObservable {
	}

	public class BeforePerformAction extends StoryTellerObservable {
	}

	public class AfterPerformAction extends StoryTellerObservable {
	}

	public class AfterStory extends StoryTellerObservable {
	}

	/*
	 * Models a visit to a point (i.e. what the teller does while processing a
	 * point)
	 */
	public class Visit implements HasProcessNode<Visit>, HasDisplayName {
		public Point point;

		Node node;

		private Iterator<Class<? extends cc.alcina.framework.gwt.client.story.Story.State>> requiresItr;

		private Iterator<? extends Point> childItr;

		Visit(Node parentNode, Point point) {
			this.node = parentNode.add(this);
			this.point = point;
		}

		Visit(Visit parent, Point point) {
			this(parent.node, point);
		}

		@Override
		public Node processNode() {
			return node;
		}

		StoryActionPerformer.Result performAction() {
			return new StoryActionPerformer().perform(this);
		}

		void add(Point point) {
			// will be added to 'initialChildren' via processNode backing
			// structure
			Visit visit = new Visit(this, point);
			if (initialChildren != null) {
				/* must also add to the traversal */
				state.onChildOfCurrentVisitAdded(visit);
			}
		}

		void populateInitialChildren() {
			List<Class<? extends Story.State>> requires = point.getRequires();
			List<? extends Point> children = point.getChildren();
			if (requires == null) {
				requires = List.of();
			}
			if (children == null) {
				children = List.of();
			}
			requiresItr = requires.iterator();
			childItr = children.iterator();
			addPending();
		}

		/*
		 * Either add the first unresolved dependency, or all children
		 * 
		 * Adding this way (rather than all at once) ensures that repeated
		 * dependencies are evaluated in the correct order
		 */
		void addPending() {
			while (requiresItr.hasNext()) {
				Class<? extends Story.State> requires = requiresItr.next();
				if (!state.isResolved(requires)) {
					add(context.resolveSatisfies(requires));
					return;
				}
			}
			while (childItr.hasNext()) {
				add(childItr.next());
			}
		}

		List<Visit> initialChildren = null;

		List<Visit> getInitialChildren() {
			if (initialChildren == null) {
				initialChildren = processNode().getChildren().stream()
						.map(n -> (Visit) n.getValue()).toList();
			}
			return initialChildren;
		}

		@Override
		public String toString() {
			return processNode().displayNamePath();
		}

		@Override
		public String displayName() {
			return Ax.blankTo(point.getName(),
					String.valueOf(processNode().indexInParent()));
		}
	}

	public class State {
		public Story story;

		DepthFirstTraversal<Visit> traversal;

		Set<Class<? extends Story.State>> resolvedStates = new LinkedHashSet<>();

		public Visit current() {
			return traversal.current();
		}

		class BeforeNodeExitListener implements TopicListener<Visit> {
			@Override
			public void topicPublished(Visit visit) {
				performAction(visit);
			}
		}

		class AtEndOfNodeChildrenListener implements TopicListener<Visit> {
			@Override
			public void topicPublished(Visit visit) {
				visit.addPending();
			}
		}

		public boolean isResolved(Class<? extends Story.State> requires) {
			return resolvedStates.contains(requires);
		}

		void onChildOfCurrentVisitAdded(Visit visit) {
			traversal.add(visit);
		}

		void init(Visit visit) {
			traversal = new DepthFirstTraversal<>(visit,
					v -> v.getInitialChildren());
			traversal.topicAtEndOfChildIterator
					.add(new AtEndOfNodeChildrenListener());
			traversal.topicBeforeNodeExit.add(new BeforeNodeExitListener());
		}

		Visit next() {
			traversal.next();
			return current();
		}

		public void dependencyResolved(Provider provider) {
			resolvedStates.add(provider.resolvesState());
		}
	}

	TellerContext context;

	State state;

	public StoryTeller(TellerContext context) {
		this.context = context;
		this.state = new State();
	}

	public void tell(Story story) {
		try {
			LooseContext.push();
			state.story = story;
			TreeProcess.Node parentNode = Registry
					.impl(TreeProcess.SelectedProcessNodeProvider.class)
					.getSelectedProcessNode();
			state.init(new Visit(parentNode, state.story.getPoint()));
			context.init(this);
			tell();
		} finally {
			LooseContext.pop();
		}
	}

	void tell() {
		new BeforeStory().publish();
		while (state.traversal.hasNext()) {
			Visit visit = state.next();
			new BeforeVisit().publish();
			visit.populateInitialChildren();
			// this will be called after children are visited. In most cases, a
			// node (visit) will either have children or an action, but there's
			// a decent case for has-dependencies-has-action
			// visit.performAction();
		}
		new AfterStory().publish();
	}

	void performAction(Visit visit) {
		new BeforePerformAction().publish();
		Result result = visit.performAction();
		if (visit.point instanceof Story.State.Provider && result.ok) {
			Story.State.Provider provider = (Provider) visit.point;
			state.dependencyResolved(provider);
		}
		new AfterPerformAction().publish();
	}
}
