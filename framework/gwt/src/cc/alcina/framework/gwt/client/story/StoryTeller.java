package cc.alcina.framework.gwt.client.story;

import java.io.PrintStream;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.process.ProcessObservers;
import cc.alcina.framework.common.client.process.TreeProcess;
import cc.alcina.framework.common.client.process.TreeProcess.HasProcessNode;
import cc.alcina.framework.common.client.process.TreeProcess.Node;
import cc.alcina.framework.common.client.process.TreeProcess.PathDisplayName;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.FormatBuilder.HardBreak;
import cc.alcina.framework.common.client.util.HasDisplayName;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.TopicListener;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;
import cc.alcina.framework.gwt.client.story.Story.Action;
import cc.alcina.framework.gwt.client.story.Story.Action.Annotate;
import cc.alcina.framework.gwt.client.story.Story.Action.Context.PerformerResource;
import cc.alcina.framework.gwt.client.story.Story.Action.Location;
import cc.alcina.framework.gwt.client.story.Story.Action.Location.Axis;
import cc.alcina.framework.gwt.client.story.Story.Attribute;
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

		public State getState() {
			return state;
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
	public class Visit
			implements HasProcessNode<Visit>, HasDisplayName, PathDisplayName {
		public Point point;

		Node node;

		private Iterator<? extends Point> childItr;

		public Result result;

		public String getDisplayName() {
			return Ax.blankTo(point.getLabel(), point.getName());
		}

		public String getLabel() {
			return point.getLabel();
		}

		public String getDescription() {
			return point.getDescription();
		}

		/**
		 * A free-form collection for say routing screenshots to the
		 * documentation generator
		 */
		public List<?> processOutputs = new ArrayList<>();

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

		/*
		 * return true if the action was performed
		 */
		boolean performAction() {
			if (result.isFiltered()) {
				return false;
				//
			} else {
				new StoryPerformer().perform(this);
				return true;
			}
		}

		void add(Point point) {
			// will be added to 'initialChildren' via processNode backing
			// structure
			Visit visit = new Visit(this, point);
			if (initialChildren != null) {
				addedChildOfCurrentVisist = true;
				/* must also add to the traversal */
				state.onChildOfCurrentVisitAdded(visit);
			}
		}

		int requiresIdx = 0;

		List<Class<? extends Story.State>> requires;

		/*
		 * For pre-story filter generation
		 */
		void populateDirectChildren() {
			List<? extends Point> children = point.getChildren();
			children.forEach(this::add);
		}

		void populateInitialChildren() {
			/*
			 * Make a copy, since it may be modified
			 */
			requires = point.getRequires().stream()
					.collect(Collectors.toList());
			List<? extends Point> children = point.getChildren();
			if (requires == null) {
				requires = List.of();
			}
			if (children == null) {
				children = List.of();
			}
			childItr = children.iterator();
			addPending();
		}

		boolean addedChildOfCurrentVisist;

		/*
		 * Either add the first unresolved dependency, or all children
		 * 
		 * Adding this way (rather than all at once) ensures that repeated
		 * dependencies are evaluated in the correct order
		 */
		void addPending() {
			while (requiresIdx < requires.size()) {
				Class<? extends Story.State> requireElement = requires
						.get(requiresIdx++);
				if (!state.isResolved(requireElement)) {
					add(context.resolveSatisfies(requireElement));
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
						.map(n -> (Visit) n.getValue())
						.collect(Collectors.toList());
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

		public Visit getParent() {
			Node rel = processNode().getParent();
			return rel == null ? null
					: rel.getValue() instanceof Visit ? rel.typedValue() : null;
		}

		public Visit getPreviousSibling() {
			Node rel = processNode().getPreviousSibling();
			return rel == null ? null : rel.typedValue();
		}

		public Action getAction() {
			return point.getAction();
		}

		public List<Annotate> getAnnotateActions() {
			return point.getAnnotateActions();
		}

		public Action.Location getLocation() {
			return point.getLocation();
		}

		public void afterActionPerformed() {
			if (point instanceof Story.State.Provider && result.ok) {
				Story.State.Provider provider = (Provider) point;
				state.dependencyResolved(provider);
			}
		}

		public class Result {
			public boolean ok = true;

			public FilteredType filteredType = FilteredType.NOT;

			public boolean isFiltered() {
				return filteredType != FilteredType.NOT;
			}

			public Boolean testResult;

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

		StoryTeller teller() {
			return StoryTeller.this;
		}

		public void onActionTestResult(boolean testResult) {
			result.testResult = testResult;
			// res
		}

		public Story.Conditional getConditional() {
			return point.getConditional();
		}

		public boolean isExitChildSequence(Visit visit) {
			if (visit.result.testResult == null) {
				return false;
			}
			if (visit.result.testResult) {
				return false;
			} else {
				return getConditional().exitOkOnFalse()
						.contains(visit.point.getClass());
			}
		}

		@Override
		public String pathDisplayName() {
			return displayName();
		}

		public void evaluateFiltered() {
			result.filteredType = filter.isFiltered(this);
		}

		/**
		 * A good place for the point to conditionally populate
		 * requires/children, post declarative population
		 */
		public void onBeforeChildren() {
			if (result.isFiltered()) {
				//
			} else {
				new StoryPerformer().beforeChildren(this);
			}
		}

		public void addRequires(Class<? extends Story.State> clazz) {
			requires.add(clazz);
			if (!addedChildOfCurrentVisist) {
				addPending();
			}
		}

		public Class<? extends Point> pointClass() {
			return point.getClass();
		}

		public class Ancestors {
			boolean includeSelf;

			public Ancestors withIncludeSelf() {
				includeSelf = true;
				return this;
			}

			public Stream<Visit> stream() {
				List<Visit> ancestors = new ArrayList<>();
				Visit cursor = Visit.this;
				while (cursor != null) {
					if (includeSelf || cursor != Visit.this) {
						ancestors.add(cursor);
					}
					cursor = cursor.getParent();
				}
				return ancestors.stream();
			}

			public boolean hasName(String name) {
				return stream().anyMatch(v -> v.displayName().equals(name));
			}
		}

		public Ancestors ancestors() {
			return new Ancestors();
		}
	}

	public enum LogType {
		PROCESS
	}

	public enum FilteredType {
		NOT,
		// a previous test correctly failed, branch should not continue
		TEST,
		// filtered via subtree (story part) filtering
		SUBTREE
	}

	public class State {
		public Story story;

		SubtreeFilter subtreeFilter = new SubtreeFilter();

		class SubtreeFilter {
			List<Class<? extends Point>> restrictionAncestors = null;

			void buildPointRestrictions() {
				if (restrictToPoint == null) {
					return;
				}
				/*
				 * Build a tree of child relations (without requires) - this
				 * will be used to skip children during main traversal
				 * 
				 * Reuse the visit logic/traversal for this
				 */
				TreeProcess.Node parentNode = Registry
						.impl(TreeProcess.SelectedProcessNodeProvider.class)
						.getSelectedProcessNode();
				Visit rootVisit = new Visit(parentNode, state.story.getPoint());
				DepthFirstTraversal<Visit> restrictionTraversal = new DepthFirstTraversal<>(
						rootVisit, v -> v.getInitialChildren());
				Stream<Visit> stream = restrictionTraversal.stream()
						.peek(v -> v.populateDirectChildren());
				Visit restrictionVisit = stream
						.filter(v -> v.pointClass() == restrictToPoint)
						.findFirst().get();
				restrictionAncestors = new ArrayList<>();
				Visit cursor = restrictionVisit;
				while (cursor != null) {
					restrictionAncestors.add(cursor.pointClass());
					cursor = cursor.getParent();
				}
			}

			/*
			 * Return false if the visit should be skipped
			 */
			boolean test(Visit visit) {
				if (restrictToPoint == null) {
					return true;
				}
				Class<? extends Point> pointClass = visit.pointClass();
				if (restrictionAncestors.contains(pointClass)) {
					return true;
				}
				Visit cursor = visit;
				do {
					if (testVisitEntry(cursor)) {
						return true;
					}
					cursor = cursor.getParent();
				} while (cursor != null);
				return false;
			}

			// if this is one of the direct ancestors *or* a dependency
			// satisfier, allow
			boolean testVisitEntry(Visit cursor) {
				return cursor.pointClass() == restrictToPoint
						|| cursor.point instanceof Story.State.Provider;
			}
		}

		long start;

		DepthFirstTraversal<Visit> traversal;

		Set<Class<? extends Story.State>> resolvedStates = new LinkedHashSet<>();

		public Visit exitVisit;

		Map<Class<? extends PerformerResource>, PerformerResource> performerResources = new LinkedHashMap();

		Map<Class<? extends Story.Attribute>, Object> attributes = new LinkedHashMap();

		Map<Location.Axis, Location> locations = new LinkedHashMap<>();

		public Visit current() {
			return traversal.current();
		}

		class BeforeNodeExitListener implements TopicListener<Visit> {
			@Override
			public void topicPublished(Visit visit) {
				updateLocation(visit);
				performAction(visit);
			}
		}

		class AtEndOfNodeChildrenListener implements TopicListener<Visit> {
			@Override
			public void topicPublished(Visit visit) {
				visit.addPending();
			}
		}

		public <T> void setAttribute(Class<? extends Story.Attribute<T>> key,
				T value) {
			attributes.put(key, value);
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

		public <PR extends PerformerResource> PR performerResource(
				Class<PR> clazz, Story.Action.Context context) {
			return (PR) performerResources.computeIfAbsent(clazz, c -> {
				PR resource = (PR) Reflections.newInstance(c);
				resource.initialise(context);
				return resource;
			});
		}

		public void updateLocationAxis(Location location) {
			locations.clear();
			locations.put(location.getAxis(), location);
		}

		public <L extends Location> L getLocation(Axis axis) {
			return (L) locations.get(axis);
		}

		public <V> Attribute.Entry<V, Attribute<V>>
				getAttribute(Class<? extends Attribute<V>> clazz) {
			V value = (V) attributes.get(clazz);
			Attribute.Entry<V, Attribute<V>> entry = new Attribute.Entry<>(
					value);
			return entry;
		}

		public void removeAttribute(Class<? extends Attribute<?>> clazz) {
			attributes.remove(clazz);
		}
	}

	TellerContext context;

	State state;

	VisitFilter filter;

	public Class<? extends Point> restrictToPoint;

	class VisitFilter {
		/*
		 * Return true if the visit is filtered (should be skipped)
		 */
		FilteredType isFiltered(Visit visit) {
			if (visit.result.isFiltered()) {
				return visit.result.filteredType;
			}
			Visit parent = visit.getParent();
			if (parent == null) {
				return FilteredType.NOT;
			}
			if (!state.subtreeFilter.test(visit)) {
				return FilteredType.SUBTREE;
			}
			if (parent.result.isFiltered()) {
				return parent.result.filteredType;
			}
			Visit previousSibling = visit.getPreviousSibling();
			if (previousSibling != null) {
				if (previousSibling.result.filteredType == FilteredType.TEST) {
					return previousSibling.result.filteredType;
				}
				if (isSequenceExit(parent, previousSibling)) {
					previousSibling.result.filteredType = FilteredType.TEST;
					return previousSibling.result.filteredType;
				}
			}
			return FilteredType.NOT;
		}

		boolean isSequenceExit(Visit parentVisit, Visit previousSiblingVisit) {
			return parentVisit.isExitChildSequence(previousSiblingVisit);
		}
	}

	public StoryTeller(TellerContext context) {
		this.context = context;
		this.state = new State();
		this.filter = new VisitFilter();
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
			state.subtreeFilter.buildPointRestrictions();
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
			if (state.exitVisit != null) {
				break;
			}
			visit.evaluateFiltered();
			new BeforeVisit().publish();
			visit.populateInitialChildren();
			// this will possibly add to the child list (but should do nothing
			// else)
			visit.onBeforeChildren();
			// visit.performAction() will be called after children are visited
			// via the depthfirsttraversal callback. In most cases, a
			// node (visit) will either have children or an action, but there's
			// a decent case for has-dependencies-has-action
			// visit.performAction();
		}
		new AfterStory().publish();
		if (state.exitVisit != null) {
			String message = Ax.format("Issue at visit %s",
					state.exitVisit.processNode().displayNamePath());
			if (context.isThrowOnFailure()) {
				throw new StoryIncomplete(message);
			} else {
				Ax.out("RESULT :: %s", message);
			}
		}
	}

	public static class StoryIncomplete extends RuntimeException {
		public StoryIncomplete(String message) {
			super(message);
		}
	}

	void updateLocation(Visit visit) {
		Location location = visit.getLocation();
		if (location != null) {
			state.updateLocationAxis(location);
		}
	}

	void performAction(Visit visit) {
		new BeforePerformAction().publish();
		if (visit.performAction()) {
			if ((visit.result.testResult != null && !visit.result.testResult)
					|| visit.result.throwable != null) {
				evaluateTestNotPassed(visit);
			}
			visit.afterActionPerformed();
		}
		new AfterPerformAction().publish();
	}

	void evaluateTestNotPassed(Visit visit) {
		if (visit.result.throwable == null
				&& visit.getParent().isExitChildSequence(visit)) {
			return;
		}
		visit.result.ok = false;
		state.exitVisit = visit;
	}

	public <T> void setAttribute(Class<? extends Story.Attribute<T>> key,
			T value) {
		state.setAttribute(key, value);
	}
}