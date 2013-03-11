package com.totsp.gwittir.client.beans;


/** A container for the properties of a Bean-type class.
 *
 * @author <a href="mailto:cooper@screaming-penguin.com">Robert "kebernet" Cooper</a>
 */
public interface BeanDescriptor {
    
    /**
     * Returns the Property instances associated with the class.
     * @return Array of Properties for the class.
     */
    Property[] getProperties();

    /**
     * Gets a property instance by name, or null if unavailable.
     * @param name Name of the property to get.
     * @return Property instance or null
     */
    Property getProperty(String name);
   
}
