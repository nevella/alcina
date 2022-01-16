## Alcina reflection branch/v2

## Goals
### Unify access - via classreflector
### Unify access restrictions - JVM follows access rules
### Simplify reflection reasoning (pruning)


## Non-goals
### Replace annotation resolver (...although maybe revisit access)

## Steps
### Implement JVM
#### Gwittir ->alcina (drop most incl introspector generator)
### Reroute access (via source modifiers)
### Working clients
### Cleanup 
### Implement GWT/.js
### Optimise GWT/.js

## FXIMES
jvm properties must ensure they have the right (sub-)type) - so the methods with object access (get, set -- getType - see methodindividual)
should route to the correct property

## plan
*	...crush it all. Delete propertyreflector, classlookup etc
*	once done, straight to registrylocation-registration, adjunct tm
*	have tm (object store) implement domain handler. clean up access
*	remove gwittir, keep only bits we like
*	annotations.resolve resolves super-method annotations (if annotation is annotated that way 
	- also defaults like @Column(or override @Column in alcina source))
*	use mvcc classes as property source (not non-mvcc superclass)
*	FIXME - reflection

## initial cyclic gwt compilation reflection plan
*	dev mode - record per-async-module reflected usages
*	script
  *	implement reflective client/server stream i/o
  *	[before reflection codegen] add classes reachable by serialization to module reachable set
  *	generate reflection code
  *	[after reflection codegen] add classes reachable by code to module reachable set
  *	[after reflection codegen] (possibly) add classes reachable by registration (module set classes) to module reachable set (and log)
  *	loop if modified in [after reflection codegen] steps

##  	TODOS
### 	reflection/script - split between serialization accessors (field) vs other (method)?