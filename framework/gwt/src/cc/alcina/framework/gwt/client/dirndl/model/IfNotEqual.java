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
 * 
 * 
 * <p>
 * The test used will be Object.equals - if the two objects are equal, the
 * corresponding property will not be set
 * 
 * <p>
 * For collection properties, you'll need to set ifNotEqual on the binding
 */
public interface IfNotEqual {
}
