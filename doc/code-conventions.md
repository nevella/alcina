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