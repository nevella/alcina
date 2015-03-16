# Introduction #

There has to be a fair amount of code interdependency for a three-tiered (client/server/entity) application - so might as well bite the bullet and call this a framework. That said, GWT/RPC borders on frameworkishness to my mind.


# Details #

The code has been designed to be mostly customisable - the basic service requirements for the transform persistence code to work are members of the following classes:

  * `ServletLayerLocator`
  * `EntityLayerLocator`
  * `CommonLocator`