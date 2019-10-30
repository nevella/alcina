package cc.alcina.framework.gwt.client.util;

import cc.alcina.framework.entity.util.RichMetadata;

import com.google.gwt.dom.client.ScriptElement;

public class MetadataInjector {
    
    RichMetadata metadata;

    ScriptElement metadataElement;

    public MetadataInjector(RichMetadata metadata) {
        this.metadata = metadata;
    }

    public RichMetadata getMetadata() {
        return metadata;
    }

    public boolean isInjected() {
        return metadataElement != null;
    }

    public void setMetadata(RichMetadata metadata) {
        if (metadataElement != null) {
            detach();
        }
        this.metadata = metadata;
    }

    public void inject() {
        if (metadataElement != null) {
            detach();
        }
        metadataElement = JavascriptInjector.injectJsonLd(metadata.getJsonld());
    }

    public void detach() {
        JavascriptInjector.removeScriptElement(metadataElement);
        metadataElement = null;
    }
}