# Introduction #

Much of the time, you won't be setting persistent object fields directly at all - since


# Details #

see [here](http://alcina.googlecode.com/svn/trunk/template/client/src/cc/alcina/template/client/widgets/BookmarksTab.java) `BookmarksTab.BookmarksHome` for a working example

##### Basic properties #####
```
Bookmark bookmark = TransformManager.get().createDomainObject(Bookmark.class);
bookmark.setParent(parent);
bookmark.setPreviousSibling(anotherBookmark);
bookmark.setUrl("http://en.wikipedia.org/wiki/Alcina");
bookmark.setTitle("Alcina - Wikipedia, the free encyclopedia");
```

> that's all - the bookmark is synchronised with storage, or queued for synchronisation if offline

##### Collection properties #####


> _the wordiness is due to the way PropertyChange events work_

```
LinkedHashSet<Bookmark> newChildren = (LinkedHashSet<Bookmark>)parent.getChildren().clone();
newChildren.add(bookmark);
parent.setChildren(newChildren);
```
_or_
```
TransformManager.get().modifyCollectionProperty(parent, "children", bookmark, CollectionModificationType.ADD);
```