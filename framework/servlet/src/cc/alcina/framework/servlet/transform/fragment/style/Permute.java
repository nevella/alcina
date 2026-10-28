package cc.alcina.framework.servlet.transform.fragment.style;
// Permute.java -- A class generating all permutations

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class Permute implements Iterator {
	public static void main(String[] args) {
		if (args.length == 0) {
			args = new String[] { "1", "2", "3", "4" };
		}
		for (Iterator i = new Permute(args); i.hasNext();) {
			final String[] a = (String[]) i.next();
			System.out.println(i);
		}
	}

	private final int size;

	private final Object[] elements; // copy of original 0 .. size-1

	private final Object ar; // array for output, 0 .. size-1

	private final int[] permutation; // perm of nums 1..size, perm[0]=0

	private boolean next = true;

	// int[], double[] array won't work :-(
	public Permute(Object[] e) {
		size = e.length;
		elements = new Object[size]; // not suitable for primitives
		System.arraycopy(e, 0, elements, 0, size);
		ar = Array.newInstance(e.getClass().getComponentType(), size);
		System.arraycopy(e, 0, ar, 0, size);
		permutation = new int[size + 1];
		for (int i = 0; i < size + 1; i++) {
			permutation[i] = i;
		}
	}

	public boolean hasNext() {
		return next;
	}

	// does not throw NoSuchElement; it wraps around!
	public Object next() throws NoSuchElementException {
		formNextPermutation(); // copy original elements
		int i = size - 1;
		while (permutation[i] > permutation[i + 1])
			i--;
		if (i == 0) {
			next = false;
			for (int j = 0; j < size + 1; j++) {
				permutation[j] = j;
			}
			return ar;
		}
		int j = size;
		while (permutation[i] > permutation[j])
			j--;
		swap(i, j);
		int r = size;
		int s = i + 1;
		while (r > s) {
			swap(r, s);
			r--;
			s++;
		}
		return ar;
	}

	public void remove() throws UnsupportedOperationException {
		throw new UnsupportedOperationException();
	}

	public String toString() {
		final int n = Array.getLength(ar);
		final StringBuffer sb = new StringBuffer("[");
		for (int j = 0; j < n; j++) {
			sb.append(Array.get(ar, j).toString());
			if (j < n - 1)
				sb.append(",");
		}
		sb.append("]");
		return new String(sb);
	}

	private void formNextPermutation() {
		for (int i = 0; i < size; i++) {
			// i+1 because perm[0] always = 0
			// perm[]-1 because the numbers 1..size are being permuted
			Array.set(ar, i, elements[permutation[i + 1] - 1]);
		}
	}

	private void swap(final int i, final int j) {
		final int x = permutation[i];
		permutation[i] = permutation[j];
		permutation[j] = x;
	}
}