# Introduction #

The core (`TransformManager`) code runs fine on any JVM (or GWT) except J2ME. See SideBenefits for usage in servlet-layer code.

Below are details for consideration in porting to other client platforms.


# Details #

### Swing ###
This may well happen as part of some other projects. It doesn't look hard - things to consider are:
  * The client will need at least gwt-servlet.jar and gwittir-trunk.jar on the classpath, in addition to alcina jars
  * Probably want to stick with Gwittir validation - and also probably follow GWT/Gwittir patterns as much as possible - use `AsyncCallback` for client/server validation, add a customiser interceptor which translates GWT customiser (widget providers) to the appropriate Swing providers.
  * Write a "JVMReflector", along the lines of `com.totsp.gwittir.client.beans.internal.JVMIntrospector`

### Dalvik/Android ###
  * As above - but (query) how memory-constrained are apps on Android likely to be, and should work be done to reduce dependencies (e.g. a minimal gwittir jar, etc) - although presumably classes which are never loaded aren't that big a problem, since the .jar itself is in flash storage