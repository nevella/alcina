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

 <h2>DEVEX styles</h2>
  <table>
  <tr>
  <td>Ordinal</td>
  <td>Meaning</td>
  </tr>
  <tr>
  <td>0</td>
  <td>Noted, no logging code</td>
  </tr>
  <tr>
  <td>1</td>
  <td>Noted, logging code</td>
  </tr>
  
  <tr>
  <td>2</td>
  <td>Noted, testing fix</td>
  </tr>
  <tr>
  <td>3</td>
  <td>(Hopefully) verified fix - should not be thrown</td>
  </tr>
  <tr>
  <td>5</td>
  <td>Unknown how it got here - just catch and log for now</td>
  </tr>
  <tr>
  <td>6</td>
  <td>Cannot reproduce</td>
  </tr>
  </table>
