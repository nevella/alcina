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
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.CachingMap;
import cc.alcina.framework.common.client.util.CommonUtils;
import cc.alcina.framework.common.client.util.FormatBuilder;
import cc.alcina.framework.common.client.util.Multimap;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class LocalDomMutations {
    private static ElementRemote findNonExternalJsAncestor(
            ElementRemote cursor) {
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

    public boolean isDisabled() {
        return this.disabled;
    }

    public void setDisabled(boolean disabled) {
        this.disabled = disabled;
    }

    public LocalDomMutations() {
    }

    public void startObserving() {
        if (this.disabled) {
            return;
        }
        if (this.observer == null) {
            setupObserver();
        }
        connectObserver();
    }

    public void stopObserving() {
        if (this.disabled) {
            return;
        }
        if (this.observer == null) {
            return;
        }
        disconnectObserver();
        checkReceivedRecords();
    }

    private native void checkReceivedRecords() /*-{
        if (this.@LocalDomMutations::records.length == 0) {
            return;
        }
        var records = this.@LocalDomMutations::records;
        this.@LocalDomMutations::records = [];
        this.@LocalDomMutations::handleMutations(*)(records);
    }-*/;

    private native void connectObserver() /*-{
        //clear the buffer and discard
        this.@LocalDomMutations::observer.takeRecords();
        this.@LocalDomMutations::records = [];

        var config = {
            attributes : true,
            childList : true,
            subtree : true
        };
        this.@LocalDomMutations::observer.observe(
                this.@LocalDomMutations::documentElement, config);
    }-*/;

    private native void disconnectObserver() /*-{
        var eventBuffer = this.@LocalDomMutations::observer.takeRecords();
        this.@LocalDomMutations::records = this.@LocalDomMutations::records
                .concat(eventBuffer);
        this.@LocalDomMutations::observer.disconnect();

    }-*/;

    private void log(Supplier<String> messageSupplier) {
        LocalDom.consoleLog(messageSupplier);
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
    void handleMutations(JsArray<MutationRecord> records) {
        try {
            handleMutations0(records);
            // LocalDom.consoleLog(Document.get().getDocumentElement().dump(true));
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    void handleMutations0(JsArray<MutationRecord> records) {
        log(() -> Ax.format("Jv records: %s", records.length()));
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
         * ** initially, just log
         */
        Multimap<NodeRemote, List<MutationRecord>> modifiedContainers = new Multimap<>();
        ClientUtils.jsArrayToTypedArray(records).forEach(record -> {
            {
                modifiedContainers.add(record.getTarget(), record);
            }
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
            Element childNodesModified = LocalDom.nodeFor(elementRemote);
            Preconditions.checkState(childNodesModified.wasResolved());
            int insertionPointLocalChildrenSize = childNodesModified
                    .getChildCount();
            long linkedToRemote = childNodesModified.streamChildren()
                    .filter(Node::linkedToRemote).count();
            List<NodeRemote> childNodesRemotePostMutation = childNodesModified
                    .typedRemote().getChildNodes0().streamRemote()
                    .collect(Collectors.toList());
            List<MutationRecord> mutationRecords = modifiedContainers
                    .get(childNodesModifiedRemote).stream().distinct()
                    .collect(Collectors.toList());
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
            Preconditions.checkState(
                    insertionPointLocalChildrenSize == preObservedSize);
            Map<NodeRemote, Node> childByRemote = new LinkedHashMap<>();
            Set<Node> retained = new LinkedHashSet<>();
            Set<Node> added = new LinkedHashSet<>();
            for (int idx = 0; idx < insertionPointLocalChildrenSize; idx++) {
                Node node = childNodesModified.getChild(idx);
                NodeRemote nodeRemote = history.preObservedState().children
                        .get(idx);
                node.putRemote(nodeRemote, true);
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
                    // created in external mutation; generate a match
                    localNode = LocalDom.resolveExternal(nodeRemote);
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

    static class ChildModificationHistory {
        private Element insertionPoint;

        List<ChildModificationHistoryState> states = new ArrayList<>();

        private List<MutationRecord> mutationRecords;

        public ChildModificationHistory(Element insertionPoint,
                List<MutationRecord> mutationRecords) {
            this.insertionPoint = insertionPoint;
            this.mutationRecords = mutationRecords;
            Collections.reverse(mutationRecords);
        }

        public void model() {
            ChildModificationHistoryState currentState = ChildModificationHistoryState
                    .fromCurrentState(insertionPoint.typedRemote());
            states.add(currentState);
            for (MutationRecord mutationRecord : mutationRecords) {
                currentState = currentState.undo(mutationRecord);
                states.add(currentState);
            }
        }

        public String toLogString() {
            FormatBuilder formatBuilder = new FormatBuilder();
            int idx = 0;
            List<MutationRecord> mutationRecordsCopy = mutationRecords.stream()
                    .collect(Collectors.toList());
            Collections.reverse(mutationRecordsCopy);
            for (MutationRecord mutationRecord : mutationRecordsCopy) {
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
        public static ChildModificationHistoryState fromCurrentState(
                ElementRemote typedRemote) {
            ChildModificationHistoryState state = new ChildModificationHistoryState();
            state.elementRemote = typedRemote;
            state.children = typedRemote.getChildNodes0().streamRemote()
                    .collect(Collectors.toList());
            return state;
        }

        MutationRecord beforeRecord;

        List<NodeRemote> children = new ArrayList<>();

        ElementRemote elementRemote;

        public ChildModificationHistoryState undo(MutationRecord record) {
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
