/**
 * <p>
 * Behaviors are small fragments of code (behavior) that run client-side,
 * including in a romcom environment. Examples are keyboard selection
 * modification, and forwarding element layout info to the romcom server
 * 
 * <p>
 * They're eesentially a have-cake-and-eat-it - where responsiveness is
 * essential (and doesn't require deep domain access), push it to the client.
 * This may be extended with custom script behaviors in the future (for say
 * multiple annotation positioning...)
 * 
 * <p>
 * Usage: elements register with one or more behaviors, and dom events are
 * previewed early and the behaviors executed if they match. The trick is
 * managing registration/de-registration - because whether an element should be
 * registered is a question for -either- the element (if it say has specific
 * attributes) or the UI framework (Dirndl) (if the model, say, implements a
 * marker interface)
 */
package com.google.gwt.dom.client.behavior;
