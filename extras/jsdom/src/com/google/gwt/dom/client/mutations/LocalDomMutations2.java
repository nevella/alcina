package com.google.gwt.dom.client.mutations;

import java.util.ArrayList;
import java.util.logging.Level;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.ElementRemote;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.MutationRecordJso;
import com.google.gwt.dom.client.mutations.MutationHistory.Event.Type;

import cc.alcina.framework.common.client.util.Ax;

public class LocalDomMutations2 {
	MutationsAccess mutationsAccess;

	private JavaScriptObject observer = null;

	private JavaScriptObject records;

	private ElementRemote documentElement;

	private boolean observerConnected = false;

	boolean enabled = true;

	MutationHistory history;

	Configuration configuration;

	boolean hadExceptions = false;

	public LocalDomMutations2(LocalDom.MutationsAccess mutationsAccess,
			Configuration configuration) {
		this.mutationsAccess = mutationsAccess;
		this.configuration = configuration;
		history = new MutationHistory(this);
	}

	public void checkDoms() {
		history.checkDoms();
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

	private native void checkReceivedRecords() /*-{
    if (this.@LocalDomMutations2::records.length == 0) {
      return;
    }
    var records = this.@LocalDomMutations2::records;
    this.@LocalDomMutations2::records = [];
    this.@LocalDomMutations2::syncMutations(*)(records);
	}-*/;

	private native void connectObserver() /*-{
    if (!this.@LocalDomMutations2::enabled) {
      var message = "Mutation tracking not defined";
      this.@LocalDomMutations2::log(Ljava/lang/String;Z)(message,true);
      return;
    }

    //clear the buffer and discard
    var mutationsList = this.@LocalDomMutations2::observer.takeRecords();
    if (mutationsList.length > 0) {
      var message = "Warning - mutation observer :: had records (was not disconnected?)";
      this.@LocalDomMutations2::log(Ljava/lang/String;Z)(message,true);
      throw message;
    }
    this.@LocalDomMutations2::records = [];

    var config = {
      childList : true,
      subtree : true,
      attributes : true,
      attributeOldValue : true,
      characterData : true,
      characterDataOldValue : true
    };
    this.@LocalDomMutations2::observer.observe(
        this.@LocalDomMutations2::documentElement, config);
    //this.@LocalDomMutations2::log(Ljava/lang/String;Z)("Mutation observer :: connected ",false);
	}-*/;

	private native void disconnectObserver() /*-{
    if (this.@LocalDomMutations2::observer == null) {
      return;
    }
    var mutationsList = this.@LocalDomMutations2::observer.takeRecords();
    var records = this.@LocalDomMutations2::records;
    mutationsList.forEach(function(mutation) {
      records.push(mutation);
    });
    if (!this.@LocalDomMutations2::observerConnected) {
      this.@LocalDomMutations2::log(Ljava/lang/String;Z)("Mutation observer :: warning  - was not connected ",true);
    }
    this.@LocalDomMutations2::observerConnected = false;
    this.@LocalDomMutations2::observer.disconnect();
    //this.@LocalDomMutations2::log(Ljava/lang/String;Z)("Mutation observer :: disconnected ",false);
	}-*/;

	private native void setupObserver() /*-{
    this.@LocalDomMutations2::enabled = this.@LocalDomMutations2::enabled
        && !(typeof MutationObserver == "undefined");
    if (!this.@LocalDomMutations2::enabled) {
      var message = "Mutation tracking not defined";
      this.@LocalDomMutations2::log(Ljava/lang/String;Z)(message,false);
      return;
    }

    this.@LocalDomMutations2::documentElement = $doc.documentElement;
    var _this = this;
    var callback = function(mutationsList, observer) {
      var records = _this.@LocalDomMutations2::records;
      mutationsList.forEach(function(mutation) {
        records.push(mutation);
      });
    };
    this.@LocalDomMutations2::observer = new MutationObserver(callback);
    var message = "Tracking remote dom mutations :: ok";
    this.@LocalDomMutations2::log(Ljava/lang/String;Z)(message,false);
	}-*/;

	private void syncMutations0(JsArray<MutationRecordJso> records) {
		SyncMutations syncMutations = new SyncMutations(mutationsAccess);
		syncMutations.sync(records);
		hadExceptions |= syncMutations.hadException;
		log(Ax.format("%s records", records.length()), false);
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
	}

	public static class Configuration {
		public boolean logDoms = true;

		public boolean logEvents = true;

		public Configuration() {
		}

		public boolean provideIsObserveHistory() {
			return logDoms || logEvents;
		}
	}
}
