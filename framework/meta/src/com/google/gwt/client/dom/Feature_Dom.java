package com.google.gwt.client.dom;

import cc.alcina.framework.common.client.meta.Feature;
import cc.alcina.framework.common.client.meta.Feature_Ui_support;

/**
 * <p>
 * The Alcina DOM implementation preserves the GWT (and W3C) api for DOM
 * manipulation, but implements that api with classes that primarily interact
 * with a 'local' (known in other systems as 'virtual') dom tree (local to the
 * VM, either jvm or js) that is synced to the browser dom.
 * 
 */
@Feature.Status.Ref(Feature.Status.In_Progress.class)
@Feature.Type.Ref(Feature.Type.Ui_support.class)
@Feature.Parent(Feature_Ui_support.class)
public interface Feature_Dom extends Feature {
}
