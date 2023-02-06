package com.google.gwt.dom.client.mutations;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.dom.client.ElementRemote;
import com.google.gwt.dom.client.LocalDom;
import com.google.gwt.dom.client.LocalDom.MutationsAccess;
import com.google.gwt.dom.client.MutationRecord;

import cc.alcina.framework.common.client.util.Ax;

public class LocalDomMutations2 {
	private MutationsAccess localDom;

	private JavaScriptObject observer = null;

	private JavaScriptObject records;

	private ElementRemote documentElement;

	private boolean observerConnected = false;

	boolean enabled = true;

	public LocalDomMutations2(LocalDom.MutationsAccess localDom) {
		this.localDom = localDom;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void startObserving() {
		if (!this.enabled) {
			return;
		}
		if (this.observer == null) {
			setupObserver();
		}
		if (!observerConnected) {
			connectObserver();
			observerConnected = true;
		} else {
			throw new IllegalStateException();
		}
	}

	public void syncMutationsAndstopObserving() {
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
      //FIXME - lcaoldom - also monitor attribute changes...maybe? wouldn't hurt for conpleteness n pretty darn easy
      subtree : true
    };
    this.@LocalDomMutations2::observer.observe(
        this.@LocalDomMutations2::documentElement, config);
    this.@LocalDomMutations2::log(Ljava/lang/String;Z)("Mutation observer :: connected ",false);
	}-*/;

	private native void consoleLog(String message, boolean error) /*-{
    if (error) {
      console.error(message);
    } else {
      console.log(message);
    }
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
      this.@LocalDomMutations2::log(Ljava/lang/String;Z)("Mutation observer :: warning  - was not connected ",false);
    }
    this.@LocalDomMutations2::observerConnected = false;
    this.@LocalDomMutations2::observer.disconnect();
    this.@LocalDomMutations2::log(Ljava/lang/String;Z)("Mutation observer :: disconnected ",false);
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

	private void syncMutations0(JsArray<MutationRecord> records) {
		log(Ax.format("%s records", records.length()), false);
	}

	void log(String message, boolean error) {
		if (error) {
			Ax.err(message);
		} else {
			Ax.out(message);
		}
		consoleLog(message, error);
	}

	// this is called at a tricky place in the GWT event loop, so make sure we
	// log exceptions
	void syncMutations(JsArray<MutationRecord> records) {
		try {
			syncMutations0(records);
		} catch (Throwable e) {
			GWT.log("Exception in handleMutations", e);
			e.printStackTrace();
			throw e;
		}
	}
}
