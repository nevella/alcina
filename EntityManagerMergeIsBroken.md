# Details #

I'm sure there's a formal way of putting it, but here's the hand-waving.

Let L be a set of live domain objects, C(L) the set of all domain objects reachable from L, D(L) be the detached projection of L (i.e. no lazy intializers, persistent sets, yadda)

  * One purpose of `EntityManager.merge()`, probably the most interesting for client/server app developers, is to allow a changed set of detached domain objects to be merged back into the central database
  * Unfortunately, unless the "changed set" is either (a) very simple or (b) closed (L=C(L) in the above terminology), there's no general way to know if, say,  a nulled property in D(L) has been clipped, or deleted. And without sending huge graphs client<>server -- or trying to keep track (on the server) of how clients obtained detached objects in the first place -- well, it gets real messy in any case.

The only real answer is to either live totally in open-JPA-session land, or use transforms as per Alcina - I can fill this in with more examples, but this one should make it clear:

```
class Group{
 Set<Group> memberGroups;
 Set<User> memberUsers;
}
class User{
Set<Group> memberOfGroups;
}
```
i.e. there's a many-many relationship between users and groups.

Say we want to make user U a member of group G. OK, add G to U.memberOfGroups and U to G.memberUsers. But ... say we have 10000 users and 500 groups. If we're in "em.merge()" land, we'd need to send basically all users and all groups back to the server - and even then it might not work, particularly for relationship removals/deletions)