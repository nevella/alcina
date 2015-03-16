## Intro ##
Been meaning to do these for a while...

## `ExtensibleEnum` ##
To handle cases where a project using this framework would probably want to extend what's currently an enum, I've created the `ExtensibleEnum` pattern (and implementation), and applied to `ContentRequestBase` and `DeliveryModel`.

`ExtensibleEnum` basically behaves like an enum (definition is unfortunately more verbose) - plugs in to the Registry (since each element is a distinct class) nicely, and is extensible.

All you need to know - [here](http://alcina.googlecode.com/git/javadoc/trunk/cc/alcina/framework/common/client/logic/ExtensibleEnum.html)

## Transform permissions ##
Object assignment is now checked for permissions - i.e., if you're assigning as follows:
```
a.b = c;
```

then you need (at least) read access to a, property write access to a.b and read access to c.

Optionally, add the new annotation `@AssignmentPermission` to control which instances of 'c' can be accessed - e.g., if you have "public groups" (which people have read access to) but people should not be able to assign those groups as field values, this annotation is how you ensure that.