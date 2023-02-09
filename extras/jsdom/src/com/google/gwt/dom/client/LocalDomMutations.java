package com.google.gwt.dom.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.util.ClientUtils;

/**
 * <p>
 * FIXME - dirndl 1x1e - stability + robustness
 *
 * <p>
 * Two things can make this fail - confusing dom changes during mutation
 * observation, or unobserved mutations during the gwt event loop
 *
 * <p>
 * The first can be at least worked on with stress testing (and fixing
 * attribute/cdata mods) - the latter is tricky. Possibly code review (all code
 * that invokes external/nonlocaldom dom modifying js should be private except
 * for public dispatch methods which just call inside LocalDom.invokeExternal
 *
 * <p>
 * There's also the complication of events in an iframe - crossframe scripting
 * issues caused by focus transfer...drift [https://www.drift.com/] widgets are
 * an example there.
 *
 * @author nick@alcina.cc
 *
 */
/*
 * @formatter:off
 * stability plan (for an alcina ticket)
    * full recording mode (don't disconnect - mark mutations as either expected or not, depending on whether within a
      com.google.gwt.dom.client.Node call or not (direct calls to NodeRemote may need to be fixed)
    * everything controlled by clientproperties
    * tools replay (in jvm)
    * (sky) possibly denote areas we care about, ignore outside - and get all <head> and <body> remotes on localdom connect
 * @formatter:on
 */
public class LocalDomMutations {
	public static LogLevel logLevel = LogLevel.NONE;

	private static ElementRemote
			findNonExternalJsAncestor(ElementRemote cursor) {
		while (!cursor.getClassName().contains("__localdom-remote-container")) {
			cursor = cursor.getParentElementRemote();
		}
		return null;
	}

	private JavaScriptObject observer = null;

	private JavaScriptObject records;

	private ElementRemote documentElement;

	private int elementUidCounter = 0;

	private boolean disabled;

	private boolean debugEntry = false;

	private boolean firstTimeConnect = true;

	private boolean observerConnected = false;

	private boolean inGwtEventCycle = false;

	public LocalDomMutations() {
	}

	public boolean isDisabled() {
		return this.disabled;
	}

	public void setDisabled(boolean disabled) {
		this.disabled = disabled;
	}

	public void startObserving() {
		if (this.disabled) {
			return;
		}
		if (this.observer == null) {
			setupObserver();
		}
		if (!observerConnected) {
			connectObserver();
			observerConnected = true;
		} else {
			clearReceivedRecords();
		}
		inGwtEventCycle = false;
	}

	public void startObservingIfNotInEventCycle() {
		if (!inGwtEventCycle) {
			startObserving();
		}
	}

	public void stopObserving() {
		if (this.disabled) {
			return;
		}
		if (this.observer == null) {
			return;
		}
		disconnectObserver();
		try {
			checkReceivedRecords();
		} catch (RuntimeException e) {
			LocalDom.onRelatedException(e);
		}
		inGwtEventCycle = true;
	}

	private native void checkReceivedRecords() /*-{
    if (this.@LocalDomMutations::records.length == 0) {
      return;
    }
    var records = this.@LocalDomMutations::records;
    this.@LocalDomMutations::records = [];
    this.@LocalDomMutations::handleMutations(*)(records);
	}-*/;

	private native void clearReceivedRecords() /*-{
    this.@LocalDomMutations::observer.takeRecords();
    this.@LocalDomMutations::records = [];

	}-*/;

	private native void connectObserver() /*-{
    if (this.@LocalDomMutations::disabled) {
      console.log("Mutation tracking not defined");
      return;
    }
    if (this.@LocalDomMutations::debugEntry
        || this.@LocalDomMutations::firstTimeConnect) {
      this.@LocalDomMutations::firstTimeConnect = false;
      this.@LocalDomMutations::logString(Ljava/lang/String;)("Mutation observer :: connected");
    }
    //clear the buffer and discard
    var mutationsList = this.@LocalDomMutations::observer.takeRecords();
    if (mutationsList.length > 0) {
      console
          .error("Warning - mutation observer :: had records (was not disconnected?)");
          this.@LocalDomMutations::logWarning(Ljava/lang/String;)
          ("Warning - mutation observer :: had records (was not disconnected?)");
    }
    this.@LocalDomMutations::records = [];

    var config = {
      childList : true,
      //FIXME - dirndl 1x1e - also monitor attribute changes...maybe? wouldn't hurt for conpleteness n pretty darn easy
      subtree : true
    };
    this.@LocalDomMutations::observer.observe(
        this.@LocalDomMutations::documentElement, config);
	}-*/;

	private native void disconnectObserver() /*-{
    var mutationsList = this.@LocalDomMutations::observer.takeRecords();
    this.@LocalDomMutations::records = this.@LocalDomMutations::records
        .concat(mutationsList);
    if (this.@LocalDomMutations::debugEntry) {
      var hadMutations = mutationsList.length > 0 ? "t" : "f";
      this.@LocalDomMutations::logString(Ljava/lang/String;)(
          "Mutation observer :: disconnected  - mutations: " + hadMutations);
    }
    if (!this.@LocalDomMutations::observerConnected
        && !this.@LocalDomMutations::firstTimeConnect) {
        	console.error("Mutation observer :: warning  - was not connected ");
      this.@LocalDomMutations::logWarning(Ljava/lang/String;)(
          "Mutation observer :: warning  - was not connected ");
    }
    this.@LocalDomMutations::observerConnected = false;
    this.@LocalDomMutations::observer.disconnect();
	}-*/;

	private void log(Supplier<String> messageSupplier) {
		// no slf4j for performance
		switch (logLevel) {
		case NONE:
			return;
		case DEV_MODE:
			System.out.println(messageSupplier.get());
			break;
		case ALL:
			System.out.println(messageSupplier.get());
			LocalDom.consoleLog0(messageSupplier.get());
			break;
		}
	}

	private void logString(String jsMessage) {
		log(() -> jsMessage);
	}

	private void logWarning(String jsMessage) {
		Ax.err(jsMessage);
	}

	private native void setupObserver() /*-{
    this.@LocalDomMutations::disabled = this.@LocalDomMutations::disabled
        || (typeof MutationObserver == "undefined");
    if (this.@LocalDomMutations::disabled) {
      console.log("Mutation tracking not defined");
      return;
    }
    this.@LocalDomMutations::documentElement = $doc.documentElement;
    var _this = this;
    var callback = function(mutationsList, observer) {
      _this.@LocalDomMutations::records = _this.@LocalDomMutations::records
          .concat(mutationsList);
    };
    this.@LocalDomMutations::observer = new MutationObserver(callback);

	}-*/;

	// this is called at a tricky place in the GWT event loop, so make sure we
	// log exceptions
	void handleMutations(JsArray<MutationRecordJso> records) {
		try {
			handleMutations0(records);
		} catch (Throwable e) {
			GWT.log("Exception in handleMutations", e);
			e.printStackTrace();
			throw e;
		}
	}

	/**
	 * <p>
	 * THe grouping of operations into the modifiedContainers structure is key.
	 * We can't just iterate through transforms because the nodes they apply to
	 * may not even exist (in the initial localdom) - so we must iterate on
	 * *final* node location. If that node is attached, we write, otherwise
	 * not...
	 */
	void handleMutations0(JsArray<MutationRecordJso> records) {
		log(() -> Ax.format("Jv records: %s", records.length()));
		String outerHtml = Document.get().typedRemote().getDocumentElement0()
				.getOuterHtml();
		/*
		 * (phase 1) - ignore attr, cdata modifications
		 *
		 * Find all parents with modified subtrees : modifiedContainers
		 *
		 * Normalise modifiedContainers so that no element of modifiedContainers
		 * is contained by another
		 *
		 * Run the localdom deltas against these modfified containers
		 *
		 */
		Multimap<NodeRemote, List<MutationRecordJso>> modifiedContainers = new Multimap<>();
		List<MutationRecordJso> typedRecords = ClientUtils
				.jsArrayToTypedArray(records);
		String combinedLogString = new ChildModificationHistory(null,
				typedRecords).toLogString();
		typedRecords.forEach(record -> {
			{
				modifiedContainers.add(record.getTarget(), record);
			}
		});
		Set<NodeRemote> normalisedModifiedContainers = new LinkedHashSet<>(
				modifiedContainers.keySet());
		Iterator<NodeRemote> itr = normalisedModifiedContainers.iterator();
		for (; itr.hasNext();) {
			boolean foundCorrespondingLocaldomNode = false;
			NodeRemote nodeRemote = itr.next();
			NodeRemote cursor = nodeRemote.getParentNodeRemote();
			while (cursor != null && cursor != documentElement
					&& cursor != Document.get().typedRemote()) {
				if (normalisedModifiedContainers.contains(cursor)) {
					itr.remove();
					break;
				}
				cursor = cursor.getParentNodeRemote();
			}
			if (cursor == null && !foundCorrespondingLocaldomNode) {
				// totally detached, so our resolution would run too early and
				// wd confuse mutation calculations
				log(() -> Ax.format("LDM - ignoring root (detached): %s",
						DomNodeStatic.shortLog(nodeRemote)));
				itr.remove();
			}
		}
		// the set is now the roots
		log(() -> Ax.format("Jv - root nodes: %s",
				normalisedModifiedContainers.size()));
		//
		// sort normalisedModifiedContainers in least->greatest depth order
		// ...shouldn't matter, but doesn't hurt
		List<NodeRemote> orderedNormalisedModifiedContainers = normalisedModifiedContainers
				.stream().sorted(new NodeDepthComparator())
				.collect(Collectors.toList());
		for (NodeRemote childNodesModifiedRemote : orderedNormalisedModifiedContainers) {
			ElementRemote elementRemote = (ElementRemote) childNodesModifiedRemote;
			List<MutationRecordJso> mutationRecords = modifiedContainers
					.get(childNodesModifiedRemote).stream()
					.collect(Collectors.toList());
			// String logString = new ChildModificationHistory(null,
			// mutationRecords).toLogString();
			// ClientUtils.invokeJsDebugger(records);
			if (!LocalDom.hasNode(elementRemote)) {
				// FIXME - dirndl 1x1e - can reproduce intermittently with drift
				// Preconditions.checkArgument(false);
				continue;
			}
			ElementRemote elt2 = elementRemote.getParentElementRemote();
			Element childNodesModified = LocalDom.nodeFor(elementRemote);
			int insertionPointLocalChildrenSize = childNodesModified
					.getChildCount();
			long linkedToRemote = childNodesModified.streamChildren()
					.filter(Node::linkedToRemote).count();
			List<NodeRemote> childNodesRemotePostMutation = childNodesModified
					.typedRemote().getChildNodes0().streamRemote()
					.collect(Collectors.toList());
			/*
			 * issue with conflicting google recaptcha and dialog insertion - if
			 * elt exists in local children, has no removes - remove adds from
			 * mutation list
			 *
			 * FIXME - dirndl 1x1e - probably issue because recaptcha called
			 * during gwt event cycle. formalise the
			 * "shouldn't modify dom during event cycle outside of localdom" -
			 * or at least wrap in connect/disconnect observer calls
			 */
			Set<NodeRemote> removed = mutationRecords.stream()
					.map(mr -> mr.getRemovedNodes())
					.flatMap(nlr -> nlr.streamRemote())
					.collect(Collectors.toSet());
			mutationRecords.removeIf(mr -> {
				if (mr.getAddedNodes().getLength() == 1) {
					NodeRemote addedRemote = mr.getAddedNodes().getItem0(0);
					if (!removed.contains(addedRemote)) {
						boolean alreadyInChildList = childNodesModified.local()
								.getChildren().stream()
								.map(child -> child.node().remote())
								.anyMatch(node -> node == addedRemote);
						if (alreadyInChildList) {
							log(() -> Ax.format(
									"removing add node from mutation list because already in localdom children - %s",
									addedRemote.hashCode()));
						}
						return alreadyInChildList;
					} else {
						return false;
					}
				} else {
					return false;
				}
			});
			ChildModificationHistory history = new ChildModificationHistory(
					childNodesModified, mutationRecords);
			history.model();
			int preObservedSize = history.preObservedState().children.size();
			log(() -> Ax.format(
					"Insertionpoint - %s - local kids: %s - local linked to remote: %s - remote kids: %s - pre-observed kids: %s",
					childNodesModified.getTagName(),
					insertionPointLocalChildrenSize, linkedToRemote,
					childNodesRemotePostMutation.size(), preObservedSize));
			log(() -> Ax.format("Local children (pre-mutation):\n%s",
					DomNodeStatic.shortLog(
							childNodesModified.local().getChildren())));
			log(() -> Ax.format("Remote children  (post-mutation):\n%s",
					DomNodeStatic.shortLog(childNodesRemotePostMutation)));
			log(() -> Ax.format("Remote children (pre-observed):\n%s",
					DomNodeStatic
							.shortLog(history.preObservedState().children)));
			log(() -> Ax.format("Mutations:\n%s", history.toLogString()));
			if (insertionPointLocalChildrenSize != preObservedSize) {
				log(() -> "...insertionPointLocalChildrenSize != preObservedSize :: throw. possible missed mutations");
				Preconditions.checkState(
						insertionPointLocalChildrenSize == preObservedSize);
			}
			Map<NodeRemote, Node> childByRemote = new LinkedHashMap<>();
			Set<Node> retained = new LinkedHashSet<>();
			Set<Node> added = new LinkedHashSet<>();
			for (int idx = 0; idx < insertionPointLocalChildrenSize; idx++) {
				Node node = childNodesModified.getChild(idx);
				NodeRemote nodeRemote = history.preObservedState().children
						.get(idx);
				boolean doNotPutOwingToRace = false;
				if (!childNodesRemotePostMutation.contains(nodeRemote)) {
					// possible hosted mode callback essentially racing this?
					if (childNodesRemotePostMutation.contains(node.remote())) {
						doNotPutOwingToRace = true;
					}
				}
				if (!doNotPutOwingToRace) {
					node.putRemote(nodeRemote, true);
				}
				if (childNodesRemotePostMutation.contains(nodeRemote)) {
					childByRemote.put(nodeRemote, node);
					retained.add(node);
				}
			}
			// remove removed nodes from localdom representation
			for (Node node : childNodesModified.streamChildren()
					.collect(Collectors.toList())) {
				if (!retained.contains(node)) {
					node.removeFromParent();
				}
			}
			int localDomIdx = 0;
			int postMutationIdx = 0;
			Node previousChild = null;
			// now insert/remove
			for (; postMutationIdx < childNodesRemotePostMutation
					.size(); postMutationIdx++) {
				NodeRemote nodeRemote = childNodesRemotePostMutation
						.get(postMutationIdx);
				Node localNode = childByRemote.get(nodeRemote);
				if (localNode == null) {
					// created (or reparented) in external mutation; generate a
					// match if new
					if (LocalDom.hasNode(nodeRemote)) {
						localNode = LocalDom.nodeFor(nodeRemote);
					}
					if (localNode == null) {
						localNode = LocalDom.resolveExternal(nodeRemote);
					}
					added.add(localNode);
				}
				Node currentLocalNode = postMutationIdx >= childNodesModified
						.getChildCount() ? null
								: childNodesModified.getChild(postMutationIdx);
				if (localNode != currentLocalNode) {
					Node insertBefore = previousChild == null ? null
							: previousChild.getNextSibling();
					if (insertBefore != null && !added.contains(insertBefore)
							&& !retained.contains(insertBefore)) {
						ClientUtils.invokeJsDebugger();
					}
					// chrome weirdness? not using insertAfter so as to debug
					if (insertBefore != null) {
						Preconditions.checkState(insertBefore
								.getParentNode() == childNodesModified);
						childNodesModified.insertBefore(localNode,
								insertBefore);
					} else {
						childNodesModified.appendChild(localNode);
					}
				}
				previousChild = localNode;
			}
			log(() -> "...mutation mirrored to localdom");
		}
	}

	public static enum LogLevel {
		NONE, DEV_MODE, ALL;
	}

	static class ChildModificationHistory {
		private Element insertionPoint;

		List<ChildModificationHistoryState> states = new ArrayList<>();

		private List<MutationRecordJso> mutationRecords;

		public ChildModificationHistory(Element insertionPoint,
				List<MutationRecordJso> mutationRecords) {
			this.insertionPoint = insertionPoint;
			this.mutationRecords = mutationRecords;
		}

		public void model() {
			ChildModificationHistoryState currentState = ChildModificationHistoryState
					.fromCurrentState(insertionPoint.typedRemote());
			states.add(currentState);
			List<MutationRecordJso> reverseMutations = mutationRecords.stream()
					.collect(Collectors.toList());
			Collections.reverse(reverseMutations);
			for (MutationRecordJso mutationRecord : reverseMutations) {
				currentState = currentState.undo(mutationRecord);
				states.add(currentState);
			}
		}

		public String toLogString() {
			FormatBuilder formatBuilder = new FormatBuilder();
			int idx = 0;
			for (MutationRecordJso mutationRecord : mutationRecords) {
				formatBuilder.line(
						"  Mutation record %s #%s %s:\n   target %s\n   pr-sib %s",
						idx++, mutationRecord.getType(),
						mutationRecord.hashCode(),
						DomNodeStatic.shortLog(mutationRecord.getTarget()),
						DomNodeStatic
								.shortLog(mutationRecord.getPreviousSibling()));
				mutationRecord.getAddedNodes().streamRemote().forEach(
						mutationNode -> formatBuilder.line("    + : %s",
								DomNodeStatic.shortLog(mutationNode)));
				mutationRecord.getRemovedNodes().streamRemote().forEach(
						mutationNode -> formatBuilder.line("    - : %s",
								DomNodeStatic.shortLog(mutationNode)));
			}
			return formatBuilder.toString();
		}

		ChildModificationHistoryState preObservedState() {
			return CommonUtils.last(states);
		}
	}

	static class ChildModificationHistoryState {
		public static ChildModificationHistoryState
				fromCurrentState(ElementRemote typedRemote) {
			ChildModificationHistoryState state = new ChildModificationHistoryState();
			state.elementRemote = typedRemote;
			state.children = typedRemote.getChildNodes0().streamRemote()
					.collect(Collectors.toList());
			return state;
		}

		MutationRecordJso beforeRecord;

		List<NodeRemote> children = new ArrayList<>();

		ElementRemote elementRemote;

		public ChildModificationHistoryState undo(MutationRecordJso record) {
			ChildModificationHistoryState undone = new ChildModificationHistoryState();
			undone.elementRemote = elementRemote;
			undone.beforeRecord = record;
			// remove first, in case of re-ordering
			undone.children = children.stream().collect(Collectors.toList());
			{
				List<NodeRemote> added = record.getAddedNodes().streamRemote()
						.collect(Collectors.toList());
				// added - so remove.
				undone.children.removeAll(added);
			}
			{
				List<NodeRemote> removed = record.getRemovedNodes()
						.streamRemote().collect(Collectors.toList());
				if (removed.size() > 0) {
					NodeRemote previousSibling = record.getPreviousSibling();
					int insertionPoint = previousSibling == null ? 0
							: children.indexOf(previousSibling) + 1;
					// removed - so add.
					for (int idx = 0; idx < removed.size(); idx++) {
						undone.children.add(insertionPoint + idx,
								removed.get(idx));
					}
				}
			}
			return undone;
		}
	}

	static class NodeDepthComparator implements Comparator<NodeRemote> {
		static int depth(NodeRemote node) {
			int depth = 0;
			NodeRemote cursor = node;
			while (cursor != null) {
				depth++;
				cursor = cursor.getParentNodeRemote();
			}
			return depth;
		}

		CachingMap<NodeRemote, Integer> depths = new CachingMap<>(
				NodeDepthComparator::depth);

		@Override
		public int compare(NodeRemote o1, NodeRemote o2) {
			return depths.get(o1) - depths.get(o2);
		}
	}
}
