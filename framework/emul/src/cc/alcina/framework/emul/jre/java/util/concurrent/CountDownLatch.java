/**
 * To allow GWT compilation of blocking async-handled tasks (although illegal on
 * the browser)
 */
public class CountDownLatch {
	public CountDownLatch() {
		throw new UnsupportedOperationException();
	}

	public CountDownLatch(int count) {
		throw new UnsupportedOperationException();
	}

	public void countDown() {
		throw new UnsupportedOperationException();
	}

	public void await() throws InterruptedException {
	}
}
