# alcina > misc > notes

## Gotchas

### Local copies of t/unsafe fields

Always, always get a local copy of a field if it's not thread-safe. The following caused me a few hours of head-scratching:

```
//cc.alcina.framework.entity.persistence.mvcc.Transactions.TransactionsStats.getTimeInVacuum()

public long getTimeInVacuum() {
	return vacuum.getVacuumStarted() == 0 ? 0
			: System.currentTimeMillis() - vacuum.getVacuumStarted();
}
```

now:

```
public long getTimeInVacuum() {
	long vacuumStarted = vacuum.getVacuumStarted();
	return vacuumStarted == 0 ? 0
			: System.currentTimeMillis() - vacuumStarted;
}
```