package cc.alcina.framework.gwt.client.util;

import com.google.gwt.dom.client.ScriptElement;

public class MetadataInjector {
	String metadata;

	ScriptElement metadataElement;

	public MetadataInjector() {
	}

	public MetadataInjector(String metadata) {
		this.metadata = metadata;
	}

	public void detach() {
		JavascriptInjector.removeScriptElement(metadataElement);
		metadataElement = null;
	}

	public String getMetadata() {
		return metadata;
	}

	public void inject() {
		if (metadataElement != null) {
			detach();
		}
		metadataElement = JavascriptInjector.injectJsonLd(metadata);
	}

	public boolean isInjected() {
		return metadataElement != null;
	}

	public void setMetadata(String metadata) {
		if (metadataElement != null) {
			detach();
		}
		this.metadata = metadata;
	}
}