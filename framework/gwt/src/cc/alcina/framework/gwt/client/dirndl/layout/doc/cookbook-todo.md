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

### Property bindings

Notes:

- Generally use ValueChange.Container (sooo simple)(see `cc.alcina.framework.servlet.component.traversal.Dotburger.Menu`)
- Describe TypedProperty usage and generation (the docs should also go to the Manifesto)

### Dialogs

[TODO] - see Overlay.java - an example:

```
Overlay.builder().withContents(editor)
.withCloseHandler(
	evt -> closeHandler.accept(editor.value.getValue()))
.build().open();
```
