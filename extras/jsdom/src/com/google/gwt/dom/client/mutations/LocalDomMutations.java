package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.ElementJso;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.mutations.MutationHistory.Event.Type;

import cc.alcina.framework.common.client.util.Ax;
import cc.alcina.framework.common.client.util.Topic;
import cc.alcina.framework.common.client.util.traversal.DepthFirstTraversal;

public class LocalDomMutations {
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

	public LocalDomMutations(LocalDom.MutationsAccess mutationsAccess,
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

	public List<MutationRecord> nodeAsMutations(Node node) {
		List<MutationRecord> records = new ArrayList<>();
		DepthFirstTraversal<Node> traversal = new DepthFirstTraversal<Node>(
				node,
				n -> n.getChildNodes().stream().collect(Collectors.toList()),
				false);
		traversal.forEach(
				n -> MutationRecord.generateInsertMutations(n, records));
		return records;
	}

	public MutationRecord nodeAsRemoveMutation(Node parent, Node oldChild) {
		return MutationRecord.generateRemoveMutation(parent, oldChild);
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

	private native void checkReceivedRecords() /*-{
    if (this.@LocalDomMutations::records.length == 0) {
      return;
    }
    var records = this.@LocalDomMutations::records;
    this.@LocalDomMutations::records = [];
    this.@LocalDomMutations::syncMutations(*)(records);
	}-*/;

	private native void connectObserver() /*-{
    if (!this.@LocalDomMutations::enabled) {
      var message = "Mutation tracking not defined";
      this.@LocalDomMutations::log(Ljava/lang/String;Z)(message,true);
      return;
    }

    //clear the buffer and discard
    var mutationsList = this.@LocalDomMutations::observer.takeRecords();
    if (mutationsList.length > 0) {
      var message = "Warning - mutation observer :: had records (was not disconnected?)";
      this.@LocalDomMutations::log(Ljava/lang/String;Z)(message,true);
      throw message;
    }
    this.@LocalDomMutations::records = [];

    var config = {
      childList : true,
      subtree : true,
      attributes : true,
      attributeOldValue : true,
      characterData : true,
      characterDataOldValue : true
    };
    this.@LocalDomMutations::observer.observe(
        this.@LocalDomMutations::documentElement, config);
    //this.@LocalDomMutations::log(Ljava/lang/String;Z)("Mutation observer :: connected ",false);
	}-*/;

	private native void disconnectObserver() /*-{
    if (this.@LocalDomMutations::observer == null) {
      return;
    }
    var mutationsList = this.@LocalDomMutations::observer.takeRecords();
    var records = this.@LocalDomMutations::records;
    mutationsList.forEach(function(mutation) {
      records.push(mutation);
    });
    if (!this.@LocalDomMutations::observerConnected) {
      this.@LocalDomMutations::log(Ljava/lang/String;Z)("Mutation observer :: warning  - was not connected ",true);
    }
    this.@LocalDomMutations::observerConnected = false;
    this.@LocalDomMutations::observer.disconnect();
    //this.@LocalDomMutations::log(Ljava/lang/String;Z)("Mutation observer :: disconnected ",false);
	}-*/;

	private native void setupObserver() /*-{
    this.@LocalDomMutations::enabled = this.@LocalDomMutations::enabled
        && !(typeof MutationObserver == "undefined");
    if (!this.@LocalDomMutations::enabled) {
      var message = "Mutation tracking not defined";
      this.@LocalDomMutations::log(Ljava/lang/String;Z)(message,false);
      return;
    }

    this.@LocalDomMutations::documentElement = $doc.documentElement;
    var _this = this;
    var callback = function(mutationsList, observer) {
      var records = _this.@LocalDomMutations::records;
      mutationsList.forEach(function(mutation) {
        records.push(mutation);
      });
    };
    this.@LocalDomMutations::observer = new MutationObserver(callback);
    var message = "Tracking remote dom mutations :: ok";
    this.@LocalDomMutations::log(Ljava/lang/String;Z)(message,false);
	}-*/;

	private void syncMutations0(JsArray<MutationRecordJso> records) {
		SyncMutations syncMutations = new SyncMutations(mutationsAccess);
		history.currentMutations = syncMutations;
		syncMutations.sync(records);
		hadExceptions |= syncMutations.hadException;
		log(Ax.format("%s records", records.length()), false);
		history.currentMutations = null;
	}

	void log(String message, boolean error) {
		LocalDom.log(error ? Level.WARNING : Level.INFO, message);
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

	public static class LoggingConfiguration {
		public boolean logDoms = false;

		public boolean logEvents = false;

		public LoggingConfiguration() {
		}

		public boolean provideIsObserveHistory() {
			return logDoms || logEvents;
		}
	}
}
