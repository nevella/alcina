### Introduction ###

This is a generalisation of annotation registry systems such as JBoss Seam and EJB3. It uses a classpath scanner for JVM containers, and even works for GWT.

### Example ###

GWT action/handler decoupling
```
@RegistryLocation(j2seOnly = false, registryPoint = PermissibleActionHandler.class, targetObject = ChangePasswordClientAction.class)
@ClientInstantiable
// TODO - i18n
public class AdminChangePasswordClientHandler implements PermissibleActionHandler {
```

JAXB context registration (very useful)
```
@RegistryLocation(registryPoint = JaxbContextRegistration.class)
public abstract class SearchDefinition extends GwtPersistableObject implements
		Serializable, TreeRenderable, ContentDefinition,
		HasPermissionsValidation {
```

### Details ###
  1. Two annotations: `@RegistryLocation and @RegistryLocations (multiple locations for one class)`
  1. Inheritance is cumulative - i.e. a subclass is registered at all RegistryLocations of its superclass chain
  1. Two basic uses:
    * Class is part of a group of classes that are interesting for some piece of  functionality - e.g. JaxbContextRegistration
    * Class is a handler for another class (which can be a superclass)
  1. Classloader-specific <br>A J2EE container actually uses two registries: <code>Registry</code> (classes visible to the entity jar) and <code>ServletLayerRegistry</code> (visible to the servlet jars).