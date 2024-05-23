# alcina > doc > code-conventions

## Use package, not private

See the beans manifesto - [/g/alcina/framework/common/src/cc/alcina/framework/common/client/reflection/package-info.java]

## Use functional internal classes

For large classes, group functionality with this pattern (this example isn't the best - but it's illustrative):

```
class Cooking{

	Oven oven(){
		return new Oven();
	}
	class Oven{
		void setTimer(){}
		void openDoor(){}
		void etc()
	}

}

```

## Use provideXx() rather than getXx/isXx for derived (non-field) properties of Entity subtypes

## and (optionally) large Bean types

(See the codebase for examples - it's to prevent unwanted serialization. @Property.Not can be used for smaller types)

## Rather than a.b.c.method() - consider creating a.method() which calls b.c

This aids encapsulation

## Don't prefix inner classes

- Top-level `Feature_ContextMenu` is fine (encouraged) (and note the underscore - indiciates namespacing prefix).
  But inner classes of `Feature_ContextMenu` should be `Copy` - not `Feature_Copy`. This is a compromise between
  IDE ease-of-location and keep-names-non-redundant (the prefix aids the former)

  Inner classes _may_ be prefixed with \_ if the class is the same name as a UI class (dev call, depends how
  often the UI class would be navigated to by name)

- Package-level (non-public) classes follow the same rules as inner classes - avoid full prefixing
