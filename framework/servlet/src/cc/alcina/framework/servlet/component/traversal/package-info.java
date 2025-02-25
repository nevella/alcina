/*
 * <p>A pluggable view of a selection traversal (see SelectionTraversal)
 * <p>Implementation note - note the override of SelectionTableArea#equals and
 * SelectionMarkupArea#equals to deal with unneeded multiple property changes
 */
@ReflectiveSerializer.Checks(ignore = true)
package cc.alcina.framework.servlet.component.traversal;

import cc.alcina.framework.common.client.serializer.ReflectiveSerializer;
import cc.alcina.framework.gwt.client.dirndl.model.IfNotEqual;;
