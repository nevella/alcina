# Relational data as code

In some situations, it makes a lot of sense to model relational data that's closely coupled to code as
code rather than as - say - json with refs, or in a relational database.

There are no clear rules to this, but some indications that it might be a useful approach are:

- The number of items to be modelled of each type fits comfortably in a java enum (say <1000)
- Each item is defined at build times (there's no user creation or anything like that)
- The items can be usefully referenced by code - not just by generic processing code, but by code
  referencing a _particular_ item

A good example satisfying all the above is the `Feature.java` feature modelling system in Alcina (and
useable in other projects) - code which satisfies/models a given feature references the `Feature` via
`@Feature.Ref` annotations - and there's a strong relational model between features and their attributes

## Patterns

### Enums and @Data

Use enums to model instances (the relational 'tables' should be enums), and
use an inner `@Data` annotation to model data - this allows defaults and named attributes rather than
opaque enum constructors - e.g.

```
public enum MyHost
...

@Data(ipAddress = "10.2.1.149", type = Type.aws_instance)
Matomo;

---not---

Matomo("10.2.1.149",Type.aws_instance)
```

If the enum size is smallish and the associated data is also, the above is fine. But for larger relational systems,
the data and enums should be detached, with something like the following, which keeps the referential nodes compact:

```
//MyClients.java
public enum MyClients


Alphabet,
Matomo,
Unicorn
;

public @interface Data
...
public @interface Ref
...
public interface Detail
...


-----------------
//Matomo.java

package foo.clients.some.client.structure;

@MyClients.Data(
	...lots
	...of
	...attributes
)
@MyClients.Ref(MyClients.Matomo)
public interface Matomo extends Detail
```

More formally:

The convention is a {@code Detail} subtype (in an app subpackage modelled
from the project category and with the same name as the enum element) has a
{@code Data} and {@code Ref} annotation, hooking up the extended type data to
the enum
