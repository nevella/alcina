# alcina > internals > caches

### Base paths - `<host-app-path>` and `<war-path>`
See [server-build.md](server-build.md) for your `<host-app-path>` - the container cache files will be visible there as well as 
from within the container. That folder is also used by the console for app metadata caching.

`<war-path>` is defined by the -war argument passed to the GWT compilation or devmode java app - e.g. 
`-war /var/local/gwt/alcina/test.app/war`. For hosted mode see your eclipse launch target or `java` shell invocation for 
the value of that argument. For script compilation, -war is blank so the war path is relative to 
the launch folder of the ant servlet compilation task (where the build.xml file is) - e.g. /alcina/app/test/servlet/war


The Webapp folders are in the container fs context, others are in the host fs context

## Classpath scanner caches

On app startup and gwt compilation, the classpath of each relevant classloader is scanned to populate the registry and provide additional
class metadata for whole-app reflective tasks. These scans are not particularly fast - wheras other (non-alcina) scanners use
byte analysis tools to filter classes of interest, the alcina ClasspathScanner loads the class - so it's important
to not load classes during the scanning phase when the class -and- its hierarchy (including interfaces) has not changed
since the last scan.

Historically, the scanner didn't check interface changes and thus was flaky (required a cache deletion to ensure interface
metadata was populated if an interface changed but the implementor didn't). That's no longer the case, but here's how to clear
the various scanner caches for completeness:


### Locations

| App     | Cache                          | Path                                                                         |
| ------- | ------------------------------ | ---------------------------------------------------------------------------- |
| Webapp  | Entity  classloader - registry | 	`/opt/jboss/.alcina/<app-server-name>/entity-layer-registry-cache.ser`       |
| Webapp  | Entity  classloader - classref | 	`/opt/jboss/.alcina/<app-server-name>/classref-scanner-cache.ser`            |
| Webapp  | Servlet classloader - registry | `/opt/jboss/.alcina/<app-server-name>/servlet-layer-registry-cache.ser`      |
| Console | Main    classloader - registry | `/<host-app-path>/consoler-registry-cache.ser`                               |
| GWT     | ---                            | (no jvm classpath scanning - see gwt sourcepath scanning below)              |


### Javadoc/Code
See `RegistryScanner`,  `ClasspathScanner`, `CachingScanner`

## GWT compilation caches

There are two sets of cached artifacts - one for hosted mode, one for devmode. These *can* get out of date - the GWT compiler 
doesn't rebuild types if their hierarchy changes (or reffed constants) - so clearing the caches  is definitely a first step 
towards resolving inexplicable exceptions in hosted or script compilations.

### Locations

| Type                |  Path                                                                         |
| ------------------- |  ---------------------------------------------------------------------------- |
| hosted              | `<war-path>/../gwt-unitCache`                                                 |
| script              | ditto, but effectively `<app-servlet-code-path/gwt-unitCache`                 |

### Javadoc/Code
See GWT `PersistentUnitCacheDir`

## Build artifacts (.class and .jar)

There are two distinct sets of .class artifacts compiled for alcina apps, the incremental (eclipse/vs.code) set and the 
full (ant compilation) set. To clean the sets: 
* incremental: eclipse/project/clean
* full: ant clean (or clean-all for all deps)

### Locations

| Type                |  Path                                                                         |
| ------------------- |  ---------------------------------------------------------------------------- |
| incremental .class  | `<project>/bin` or `<project>/<subproject>/bin`                               |
| incremental .jar    | (none)                                                                        |
| full        .class  | `<project>/build` or `<project>/<subproject>/build`                           |
| full        .jar    | `<project>/dist`                                                              |
