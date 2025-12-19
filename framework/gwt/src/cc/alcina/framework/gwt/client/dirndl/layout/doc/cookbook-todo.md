# Uncategorised thoughts (so far)

## Events

### Listener collision

If two child properties fire the same event (say two Choices properties), there are two options:

- Differentiate handling based on the firer:

```
@Override
public void onSelectionChanged(SelectionChanged event) {
	if (event.getContext().node.getModel() == model0) {
	}
	..else model1
	{

	}

```

- Wrap one or more properties in a @Directed.Delegating wrapper (essentially just a listener)

```
@Directed(renderer=DirectedRenderer.Delegating.class,receives=ModelEvents.SelectionChanged.class)
public static class Foo extends Model implements SelectionChanged.Handler{

	@Override
	public void onSelectionChanged(SelectionChanged event) {
	}
}
```

This is, admittedly, more verbose than listeners-as-lambdas -- but so far in 3 large applications I've had to
do this exactly once

### Re-emission of a received event

If a model receives and then re-emits a model event (with the same model class), it must check to avoid a circular loop

e.g. cc.alcina.framework.gwt.client.dirndl.model.Choices.Single.onSelected(Selected)

```
@Override
public void onSelected(Selected event) {
	if (event.getContext().node == node) {
		return;
	}
```

### Property bindings (binding model properties to transformed model values)

Notes:

- Generally have the containing model implement ValueChange.Container (sooo simple)(see `cc.alcina.framework.servlet.component.traversal.Dotburger.Menu`)
- Describe TypedProperty usage and generation (the docs should also go to the Manifesto)
- Or encapsulate the properties ina form model (if validation etc required)

### Dialogs

[TODO] - see Overlay.java - an example:

```
Overlay.builder().withContents(editor)
.withCloseHandler(
	evt -> closeHandler.accept(editor.value.getValue()))
.build().open();
```

### Composition and services

SequenceArea is an example of using services rather than DirectedContextResolver to customise a container.

The basic philosophy is: 'if you expect customisation, access the expected customisable features via a service' - otherwise
fall back on the more general (but less strongly typed and more brittle) resolver operations such as annotation resolution.
