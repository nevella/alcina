/*
 * BoundWidgetProvider.java
 *
 * Created on June 14, 2007, 4:28 PM
 *
 * To change this template, choose Tools | Template Manager and open the
 * template in the editor.
 */
package cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui.util;

import cc.alcina.framework.gwt.shadow.com.totsp.gwittir.client.ui.BoundWidget;

/**
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet"
 *         Cooper</a>
 */
public interface BoundWidgetProvider {
	public BoundWidget<?> get();
}
