package cc.alcina.framework.gwt.client.story;

import java.io.PrintStream;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
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
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.FormatBuilder.HardBreak;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.story.Story.Action;
import cc.alcina.framework.gwt.client.story.Story.Point;
import cc.alcina.framework.gwt.client.story.Story.State.Provider;
import cc.alcina.framework.gwt.client.story.StoryTeller.Visit.Result.Log;

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

		public Result result;

		Visit(Node parentNode, Point point) {
			this.node = parentNode.add(this);
			this.point = point;
			result = new Result();
		}

		Visit(Visit parent, Point point) {
			this(parent.node, point);
		}

		@Override
		public Node processNode() {
			return node;
		}

		Visit.Result performAction() {
			new StoryActionPerformer().perform(this);
			return result;
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

		public Action getAction() {
			return point.getAction();
		}

		public void afterActionPerformed(Result result) {
			if (point instanceof Story.State.Provider && result.ok) {
				Story.State.Provider provider = (Provider) point;
				state.dependencyResolved(provider);
			}
		}

		public class Result {
			public boolean ok = true;

			public Throwable throwable;

			public List<Log> logs = new ArrayList<>();

			Result() {
			}

			/**
			 * The pattern used to emit log records is call this method,
			 * populate the entry and call log - e.g.
			 * 
			 * <pre>
			 * <code>
			 * visit.result.logEntry().level(level).template(template).args(args)
					.log();
			 * </code>
			 * </pre>
			 * 
			 * @return the Log.Builder builder instance
			 */
			public Log.Builder logEntry() {
				Log log = new Log();
				logs.add(log);
				return log.builder();
			}

			public class Log {
				Log() {
				}

				public long time;

				public System.Logger.Level level = Level.INFO;

				public Throwable throwable;

				public String message;

				List<LogType> types;

				public Builder builder() {
					return new Builder();
				}

				public class Builder {
					private Object[] args;

					private String template;

					public Builder level(Level level) {
						Log.this.level = level;
						return this;
					}

					public Builder throwable(Throwable throwable) {
						Log.this.throwable = throwable;
						return this;
					}

					public Builder template(String template) {
						this.template = template;
						return this;
					}

					public Builder args(Object... args) {
						this.args = args;
						return this;
					}

					public Builder types(LogType... types) {
						Log.this.types = List.of(types);
						return this;
					}

					public void log() {
						Log.this.time = System.currentTimeMillis();
						Log.this.message = Ax.format(template, args);
						echo(Log.this);
					}
				}

				public Visit getVisit() {
					return Visit.this;
				}

				public boolean hasType(LogType type) {
					return types != null && types.contains(type);
				}
			}
		}

		public int depth() {
			return processNode().depth();
		}
	}

	public enum LogType {
		PROCESS
	}

	public class State {
		public Story story;

		long start;

		DepthFirstTraversal<Visit> traversal;

		Set<Class<? extends Story.State>> resolvedStates = new LinkedHashSet<>();

		public boolean finished;

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

	public void echo(Log log) {
		int depth = log.getVisit().depth() - 1;
		FormatBuilder format = new FormatBuilder();
		format.withTrackNewlines(true);
		int treeLength = 110;
		if (!log.hasType(LogType.PROCESS)) {
			depth++;
		}
		format.indent(depth * 2);
		HardBreak hardBreak = new FormatBuilder.HardBreak(log.message,
				treeLength - depth * 2);
		format.append(hardBreak.lines.get(0));
		format.padTo(treeLength);
		format.appendPadLeft(8, log.time - state.start);
		format.format("  %s", log.level);
		format.newLine();
		hardBreak.lines.stream().skip(1).forEach(format::line);
		PrintStream out = System.out;
		if (log.level.getSeverity() >= Level.WARNING.getSeverity()) {
			out = System.err;
		}
		out.print(format);
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
			System.out.println();
			tell();
			System.out.println();
		} finally {
			LooseContext.pop();
		}
	}

	void tell() {
		state.start = System.currentTimeMillis();
		new BeforeStory().publish();
		while (state.traversal.hasNext()) {
			Visit visit = state.next();
			if (state.finished) {
				break;
			}
			new BeforeVisit().publish();
			visit.populateInitialChildren();
			// visit.performAction() will be called after children are visited
			// via the depthfirsttraversal callback. In most cases, a
			// node (visit) will either have children or an action, but there's
			// a decent case for has-dependencies-has-action
			// visit.performAction();
		}
		new AfterStory().publish();
	}

	void performAction(Visit visit) {
		new BeforePerformAction().publish();
		Visit.Result result = visit.performAction();
		if (!result.ok) {
			state.finished = true;
		}
		visit.afterActionPerformed(result);
		new AfterPerformAction().publish();
	}
}
