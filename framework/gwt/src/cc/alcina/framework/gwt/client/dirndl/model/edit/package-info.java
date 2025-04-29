/**
 * <h2>Dirndl editor models</h2>
 *
 * <h3>Decorators</h3>
 * <p>
 * The decorator system supports user decoration of a contentEditable dom
 * fragment. The decoration process itself is documented in
 * {@link ContentDecorator}, this is a rough breakdown of the roles of the
 * classes used:
 * </p>
 * <ul>
 * <li>{@link HasDecorators} - the top model of the editable fragment,
 * responsible for setting up {@link ContentDecorator} instances, routing dom
 * events nnd hosting the {@code contenteditable } dom node
 * <li>{@link FragmentModel} - maintains a bidi mapping between the dom
 * structure and the {@link FragmentNode} model structure
 * <li>{@link EntityNode} - an abstract FragmentNode subclass modelling a
 * decorator, such as a hash tag or a user mention
 * <li>{@link EntityNode.Descriptor} - describes the characteristics of a
 * decorator node subclass, such as the triggering string ('#' or '@') and the
 * renderer which maps what the user decorates the node <i>with> to a string
 * <li>{@link ContentDecorator} - (one per decorator node type) responsible for
 * converting a trigger string to a decorator node, showing the decorator
 * dropdown and routing events
 *
 * </ul>
 * 
 * *
 * <h3>Behaviors</h3>
 * <p>
 * Because the interaction of moving parts to support editor/decorator behavior
 * is so complex, the documentation and reasoning of the various behaviors is
 * modelled as inner classes of {@link DecoratorBehavior} - at times, other
 * classes/methods will reference a logical behavior where the actual
 * implementation is a method (or part of a method) in another class
 */
package cc.alcina.framework.gwt.client.dirndl.model.edit;
