package com.google.gwt.dom.client.mutations;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeRemote;

import cc.alcina.framework.common.client.util.AlcinaCollections;
import cc.alcina.framework.gwt.client.util.ClientUtils;

class SyncMutations {
	MutationsAccess mutationsAccess;

	Map<NodeRemote, MutationNode> mutationNodes = AlcinaCollections
			.newUnqiueMap();

	public SyncMutations(MutationsAccess mutationsAccess) {
		this.mutationsAccess = mutationsAccess;
	}

	public MutationNode mutationNode(NodeRemote nodeRemote) {
		return nodeRemote == null ? null
				: mutationNodes.computeIfAbsent(nodeRemote,
						n -> new MutationNode(n, this, mutationsAccess, false,
								null));
	}

	public void sync(JsArray<MutationRecordJso> records) {
		List<MutationRecordJso> recordJsoList = ClientUtils
				.jsArrayToTypedArray(records);
		recordJsoList.stream().map(jso -> new MutationRecord(this, jso))
				.collect(Collectors.toList());
		// create
	}

	public NodeRemote typedRemote(Node n) {
		return mutationsAccess.typedRemote(n);
	}
}
