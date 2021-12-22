## Alcina reflection branch/v2

## Goals
### Unify access - via classreflector
### Unify access restrictions - JVM follows access rules
### Simplify reflection reasoning (pruning)


## Non-goals
### Replace annotation resolver (...although maybe revisit access)

## Steps
### Implement JVM
### Reroute access (via source modifiers)
### Working clients
### Cleanup 
### Implement GWT/.js
### Optimise GWT/.js

## FXIMES
jvm properties must ensure they have the right (sub-)type) - so the methods with object access (get, set -- getType - see methodindividual)
should route to the correct property

## plan
...crush it all. Delete propertyreflector, classlookup etc