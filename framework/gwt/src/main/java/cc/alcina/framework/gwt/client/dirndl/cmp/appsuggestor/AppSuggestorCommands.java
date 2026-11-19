package cc.alcina.framework.gwt.client.dirndl.cmp.appsuggestor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import cc.alcina.framework.common.client.logic.reflection.Registration;
import cc.alcina.framework.common.client.logic.reflection.registry.Registry;
import cc.alcina.framework.common.client.reflection.Reflections;
import cc.alcina.framework.common.client.util.AlcinaCollectors;
import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

@Registration.Singleton
public class AppSuggestorCommands {
	public static AppSuggestorCommands get() {
		return Registry.impl(AppSuggestorCommands.class);
	}

	List<AppSuggestorCommands.CommandNode> roots;

	/*
	 * Construct a lookup table from all registrable ModelEvent classes
	 */
	public AppSuggestorCommands() {
		List<AppSuggestorCommands.CommandNode> nodes = Registry
				.query(AppSuggestorEvent.class).registrations()
				.filter(c -> Reflections.at(c).has(AppSuggestorCommand.class))
				.map(AppSuggestorCommands.CommandNode::new)
				.collect(Collectors.toList());
		Map<Class<? extends AppSuggestorEvent>, AppSuggestorCommands.CommandNode> byModelClass = nodes
				.stream().collect(AlcinaCollectors.toKeyMap(c -> c.eventClass));
		nodes.forEach(node -> {
			Class<? extends AppSuggestorEvent> parent = node.command.parent();
			if (byModelClass.containsKey(parent)) {
				AppSuggestorCommands.CommandNode parentNode = byModelClass
						.get(parent);
				node.parent = parentNode;
				parentNode.children.add(node);
			}
		});
		nodes.forEach(node -> Collections.sort(node.children));
		roots = nodes.stream().filter(node -> node.parent == null).sorted()
				.collect(Collectors.toList());
	}

	public List<AppSuggestorCommands.CommandNode> getCommandNodes(
			AppSuggestorRequest request, MatchStyle matchStyle) {
		MatchBranch root = new MatchBranch(request, matchStyle);
		root.match();
		return root.stream().filter(MatchBranch::hasConcreteMatch)
				.map(MatchBranch::node).collect(Collectors.toList());
	}

	public static class CommandNode
			implements Comparable<AppSuggestorCommands.CommandNode> {
		public Class<? extends AppSuggestorEvent> eventClass;

		AppSuggestorCommands.CommandNode parent;

		List<AppSuggestorCommands.CommandNode> children = new ArrayList<>();

		public AppSuggestorCommand command;

		String name;

		public CommandNode(Class<? extends AppSuggestorEvent> eventClass) {
			this.eventClass = eventClass;
			this.command = Reflections.at(eventClass)
					.annotation(AppSuggestorCommand.class);
			this.name = command.name().toLowerCase();
		}

		@Override
		public int compareTo(AppSuggestorCommands.CommandNode o) {
			return name.compareTo(o.name);
		}

		public String toPath() {
			List<String> parts = new LinkedList<>();
			CommandNode cursor = this;
			while (cursor != null) {
				parts.add(0, cursor.name);
				cursor = cursor.parent;
			}
			return parts.stream().collect(Collectors.joining(" - "));
		}

		@Override
		public String toString() {
			return Ax.format("%s - %s", name, getClass().getSimpleName());
		}

		boolean matches(MatchBranch matchBranch) {
			if (!matchBranch.matchesName(name)) {
				return false;
			}
			return AppSuggestorCommand.Support.testFilter(eventClass, command);
		}
	}

	public enum MatchStyle {
		initial_substring, any_substring
	}

	/*
	 * Models a partial text query match against the command node tree
	 */
	class MatchBranch {
		List<String> spaceSeparatedTokens;

		AppSuggestorRequest request;

		MatchBranch parent;

		List<MatchBranch> children = new ArrayList<>();

		AppSuggestorCommands.CommandNode node;

		MatchStyle matchStyle;

		boolean hasMatch;

		MatchStyle matchStyle() {
			return root().matchStyle;
		}

		public boolean matchesName(String name) {
			String firstToken = firstToken();
			return name.startsWith(firstToken.toLowerCase());
		}

		MatchBranch root() {
			MatchBranch cursor = this;
			while (cursor.parent != null) {
				cursor = cursor.parent;
			}
			return cursor;
		}

		MatchBranch(MatchBranch parent, AppSuggestorCommands.CommandNode node) {
			// the child tokens (to match) should be the parent tokens, but
			// remove one if the parent consumed it (had match)
			spaceSeparatedTokens = parent.hasMatch
					? parent.spaceSeparatedTokens.subList(1,
							parent.spaceSeparatedTokens.size())
					: parent.spaceSeparatedTokens;
			this.request = parent.request;
			this.parent = parent;
			boolean commonContext = Arrays
					.stream(AppSuggestorCommand.Support.contexts(node.command))
					.anyMatch(request.commandContexts::contains);
			if (!commonContext) {
				return;
			}
			hasMatch = node != null
					&& (spaceSeparatedTokens.size() == 0 || node.matches(this));
			boolean continueMatchPath =
					// we've consumed all tokens, so match all descendants
					hasMatch || matchStyle() == MatchStyle.any_substring;
			if (continueMatchPath) {
				this.node = node;
				parent.children.add(this);
			}
		}

		/*
		 * Creates a root branch, which matches - say "app - reload" (command
		 * app/command reload) against 'a r' (initial substring) or 'r' (any
		 * substring)
		 */
		MatchBranch(AppSuggestorRequest request, MatchStyle matchStyle) {
			this.request = request;
			this.matchStyle = matchStyle;
			spaceSeparatedTokens = List
					.of(request.getQuery().toLowerCase().split(" "));
		}

		public List<MatchBranch> branchPath() {
			List<MatchBranch> branches = new ArrayList<>();
			MatchBranch cursor = this;
			do {
				branches.add(cursor);
				cursor = cursor.parent;
			} while (cursor != null);
			return branches;
		}

		@Override
		public String toString() {
			return Ax.format("%s - %s", spaceSeparatedTokens, node);
		}

		String firstToken() {
			return Ax.first(spaceSeparatedTokens);
		}

		boolean hasConcreteMatch() {
			return node != null && !Reflections.at(node.eventClass).isAbstract()
					&& pathHasMatch();
		}

		boolean pathHasMatch() {
			MatchBranch cursor = this;
			do {
				if (cursor.hasMatch) {
					return true;
				}
				cursor = cursor.parent;
			} while (cursor != null);
			return false;
		}

		void match() {
			List<CommandNode> nodes = node == null ? roots : node.children;
			// if it matches, the new match instance will be added to
			// this.children
			nodes.stream().forEach(n -> new MatchBranch(this, n));
			children.forEach(MatchBranch::match);
		}

		AppSuggestorCommands.CommandNode node() {
			return node;
		}

		Stream<MatchBranch> stream() {
			return new DepthFirstTraversal<>(this, b -> b.children).stream();
		}
	}
}