# Naming notes/patterns

## Builder/Attributes

When an object (for instance the Dirndl Suggestor component) has a complex initial configuration,
the configuration object is both the Builder for the object and the configuration accessor once
the instance has been created - and is called the object's Attributes

It's very similar to React Props - in that it's intended immutable. It's not _called_ that because
the word "property" is so overused in Alcina - also because in DOM 'property' is somewhat - a bit -
more mutable than Attribute, and the intention is immutability.

There's no really good English word for "immutable initial characteristics which define a thing" -
although "trait" probably comes closer. But it doesn't feel right...
