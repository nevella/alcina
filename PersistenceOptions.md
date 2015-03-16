# Introduction #

User properties and search definitions are generally better persisted as single wrapped objects rather than sometimes-massive graphs. Nevertheless, simple (enum/string/primitive) properties are transparently persisted via the `TransformManager` - and more complex objects via `CommonPersistenceService.merge()`


# Details #

For examples of the first, in the template application,  create a group named "Developers", add yourself to it, and then click on "Options" - the `cc.alcina.framework.gwt.client.data.GeneralProperties` object is persisted (in the db) via xml wrapping.

![http://alcina.googlecode.com/svn/trunk/doc/img/create-developers-group.png](http://alcina.googlecode.com/svn/trunk/doc/img/create-developers-group.png)

![http://alcina.googlecode.com/svn/trunk/doc/img/dev-options.png](http://alcina.googlecode.com/svn/trunk/doc/img/dev-options.png)
