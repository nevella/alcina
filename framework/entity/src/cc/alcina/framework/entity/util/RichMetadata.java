package cc.alcina.framework.entity.util;

import com.google.schemaorg.JsonLdSerializer;
import com.google.schemaorg.core.Thing;

import cc.alcina.framework.common.client.WrappedRuntimeException;

public abstract class RichMetadata {
    private String jsonld;

    public Thing obj;

    public RichMetadata() {
    }

    public abstract void build();

    public void generateJsonld() {
        try {
            jsonld = new JsonLdSerializer(true).serialize(obj);
        } catch (Exception e) {
            throw new WrappedRuntimeException(e);
        }
    }

    public String getJsonld() {
        if (jsonld != null) {
            generateJsonld();
        }
        return jsonld;
    }

    public Thing getObject() {
        return obj;
    }

    @Override
    public String toString() {
        return getJsonld();
    }

}
