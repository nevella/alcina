package cc.alcina.framework.gwt.client.data.view.res;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

import cc.alcina.framework.gwt.client.gen.SimpleCssResource;

public interface DataClientResources extends ClientBundle {
    public static final DataClientResources INSTANCE = GWT
            .create(DataClientResources.class);

    @Source("dataclient-alcina.css")
    public SimpleCssResource alcinaCss();

    @Source("dataclient.css")
    public SimpleCssResource css();

    @Source("dataclient-complex.css")
    public SimpleCssResource cssComplex();

    @Source("transparent.png")
    ImageResource transparent();
}