package cc.alcina.framework.gwt.client.dirndl.model;

/**
 * <p>
 * Allow ui property changes to be rejected if the current value is 'good
 * enough' - i.e. a property may have changed, but the object has already been
 * rendered embodying that property (for example, when multiple/cascading
 * property changes occur).
 * 
 * <p>
 * This is a fairly key UI optimisation - see the Traversal Browser and
 * integration with {@link ModelBinding}
 * 
 * <p>
 * I - the input type (bean), not the input property type. Note that if the
 * target property is bound to input properties from different sources, those
 * sources will all need to implement a common interface (such as HasPage in the
 * TraversalBrowser)
 */
public interface IfNotExisting<I> {
	boolean testExistingSatisfies(I input);
}
