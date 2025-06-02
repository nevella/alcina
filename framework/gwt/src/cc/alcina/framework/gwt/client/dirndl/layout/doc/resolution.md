# Resolution notes (resolution debugging)

Debugging tree-table annotation resolution:

## FmsValueModel is not used as the source of @Directed annotations for TableValueModel

(but it is for SequenceBrowser)

...debug where it's sourced:

```
Class<? extends DirectedRenderer> cc.alcina.framework.gwt.client.dirndl.annotation.Directed.Impl.renderer()

@Override
public Class<? extends DirectedRenderer> renderer() {
	if (renderer.getName().contains("FmsValueRenderer")) {
		int debug = 3;
	}
```

caused by merge in mergeParent

which is because of custom resolveAnnotations0Super (which sends up)

protected <A extends Annotation> List<A> resolveAnnotations0Super(
Class<A> annotationClass, AnnotationLocation location) {
return super.resolveAnnotations0(annotationClass, location);
}
