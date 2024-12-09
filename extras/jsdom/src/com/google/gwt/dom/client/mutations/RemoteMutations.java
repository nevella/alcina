package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ElementJso;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationHistory.Event.Type;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.LooseContext;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.gwt.client.util.ClientUtils;

public class RemoteMutations {
	/*
	 * Dom inserts can be modelled as a series of inserts (one per node, incl
	 * attributes) - or as the outer markup of the inserted node.
	 * 
	 * Default to the latter for performance (although slightly riskier for
	 * non-standard dom/browser parsing)
	 * 
	 * FIMXE - romcom.2.1 - hookup to generalised docs re
	 * mutation/transport/browser parsing/xhtml etc
	 */
	MutationsAccess mutationsAccess;

	private JavaScriptObject observer = null;

	private JavaScriptObject records;

	private ElementJso documentElement;

	private boolean observerConnected = false;

	boolean enabled = true;

	MutationHistory history;

	LoggingConfiguration loggingConfiguration;

	boolean hadExceptions = false;

	Topic<Void> topicMutationOccurred = Topic.create();

	public RemoteMutations(LocalDom.MutationsAccess mutationsAccess,
			LoggingConfiguration configuration) {
		this.mutationsAccess = mutationsAccess;
		this.loggingConfiguration = configuration;
		history = new MutationHistory(this);
	}

	public void applyDetachedMutations(List<MutationRecord> mutations,
			boolean applyToRemote) {
		SyncMutations syncMutations = new SyncMutations(mutationsAccess);
		syncMutations.applyDetachedMutationsToLocalDom(mutations,
				applyToRemote);
	}

	private native void checkReceivedRecords() /*-{
    if (this.@RemoteMutations::records.length == 0) {
      return;
    }
    var records = this.@RemoteMutations::records;
    this.@RemoteMutations::records = [];
    this.@RemoteMutations::syncMutations(*)(records);
	}-*/;

	private native void connectObserver() /*-{
    if (!this.@RemoteMutations::enabled) {
      var message = "Mutation tracking not defined";
      this.@RemoteMutations::log(Ljava/lang/String;Z)(message,true);
      return;
    }

    //clear the buffer and discard
    var mutationsList = this.@RemoteMutations::observer.takeRecords();
    if (mutationsList.length > 0) {
      var message = "Warning - mutation observer :: had records (was not disconnected?)";
      this.@RemoteMutations::log(Ljava/lang/String;Z)(message,true);
      throw message;
    }
    this.@RemoteMutations::records = [];

    var config = {
      childList : true,
      subtree : true,
      attributes : true,
      attributeOldValue : true,
      characterData : true,
      characterDataOldValue : true
    };
    this.@RemoteMutations::observer.observe(
        this.@RemoteMutations::documentElement, config);
    //this.@RemoteMutations::log(Ljava/lang/String;Z)("Mutation observer :: connected ",false);
	}-*/;

	private native void disconnectObserver() /*-{
    if (this.@RemoteMutations::observer == null) {
      return;
    }
    var mutationsList = this.@RemoteMutations::observer.takeRecords();
    var records = this.@RemoteMutations::records;
    mutationsList.forEach(function(mutation) {
      records.push(mutation);
    });
    if (!this.@RemoteMutations::observerConnected) {
      this.@RemoteMutations::log(Ljava/lang/String;Z)("Mutation observer :: warning  - was not connected ",true);
    }
    this.@RemoteMutations::observerConnected = false;
    this.@RemoteMutations::observer.disconnect();
    //this.@RemoteMutations::log(Ljava/lang/String;Z)("Mutation observer :: disconnected ",false);
	}-*/;

	public boolean hadExceptions() {
		return history.hadExceptions() || hadExceptions;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public boolean isHadExceptions() {
		return this.hadExceptions;
	}

	public boolean isObserverConnected() {
		return this.observerConnected;
	}

	void log(String message, boolean error) {
		LocalDom.log(error ? Level.WARNING : Level.INFO, message);
	}

	public List<MutationRecord> nodeAsMutations(Node node, boolean deep) {
		List<MutationRecord> records = new ArrayList<>();
		if (deep) {
			try {
				LooseContext.push();
				MutationRecord.deltaFlag(
						MutationRecord.FlagTransportMarkupTree.class, true);
				MutationRecord.generateInsertMutations(node, records);
			} finally {
				LooseContext.pop();
			}
		} else {
			// just this one, no inner markup
			MutationRecord.generateInsertMutations(node, records);
		}
		return records;
	}

	public MutationRecord nodeAsRemoveMutation(Node parent, Node oldChild) {
		MutationRecord removeMutation = MutationRecord
				.generateRemoveMutation(parent, oldChild);
		mutationsAccess
				.applyPreRemovalAttachId(removeMutation.removedNodes.get(0));
		return removeMutation;
	}

	public String serializeHistory() {
		return history.serialize();
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setHadExceptions(boolean hadExceptions) {
		this.hadExceptions = hadExceptions;
	}

	public void setObserverConnected(boolean observerConnected) {
		this.observerConnected = observerConnected;
	}

	private native void setupObserver() /*-{
    this.@RemoteMutations::enabled = this.@RemoteMutations::enabled
        && !(typeof MutationObserver == "undefined");
    if (!this.@RemoteMutations::enabled) {
      var message = "Mutation tracking not defined";
      this.@RemoteMutations::log(Ljava/lang/String;Z)(message,false);
      return;
    }

    this.@RemoteMutations::documentElement = $doc.documentElement;
    var _this = this;
    var callback = function(mutationsList, observer) {
      var records = _this.@RemoteMutations::records;
      mutationsList.forEach(function(mutation) {
        records.push(mutation);
      });
    };
    this.@RemoteMutations::observer = new MutationObserver(callback);
    var message = "Tracking remote dom mutations :: ok";
    this.@RemoteMutations::log(Ljava/lang/String;Z)(message,false);
	}-*/;

	public void startObserving() {
		if (!this.enabled) {
			return;
		}
		if (this.observer == null) {
			setupObserver();
			MutationHistory.Event.publish(Type.INIT, new ArrayList<>());
		}
		if (!observerConnected) {
			connectObserver();
			observerConnected = true;
		} else {
			throw new IllegalStateException();
		}
	}

	// this is called at a tricky place in the GWT event loop, so make sure we
	// log exceptions
	void syncMutations(JsArray<MutationRecordJso> records) {
		try {
			syncMutations0(records);
		} catch (Throwable e) {
			GWT.log("Exception in handleMutations", e);
			e.printStackTrace();
			throw e;
		}
		topicMutationOccurred.signal();
	}

	private void syncMutations0(JsArray<MutationRecordJso> records) {
		SyncMutations syncMutations = new SyncMutations(mutationsAccess);
		history.currentMutations = syncMutations;
		List<MutationRecord> recordList = syncMutations.sync(records);
		hadExceptions |= syncMutations.hadException;
		log(Ax.format("%s records", records.length()), false);
		history.currentMutations = null;
		mutationsAccess.onRemoteMutationsApplied(recordList,
				syncMutations.hadException);
	}

	public void syncMutationsAndStopObserving() {
		if (!this.enabled) {
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
	}

	public void verifyDomEquivalence() {
		history.verifyDomEquivalence();
	}

	public static class LoggingConfiguration {
		public boolean logDoms = false;

		public boolean logEvents = false;

		public LoggingConfiguration() {
		}

		public boolean provideIsObserveHistory() {
			return logDoms || logEvents;
		}
	}

	public void emitInnerMarkupMutation(Element elem) {
		MutationRecord mutation = MutationRecord
				.generateMarkupMutationRecord(elem);
		Document.get().implAccess().attachIdRemote().emitMutation(mutation);
	}
}
