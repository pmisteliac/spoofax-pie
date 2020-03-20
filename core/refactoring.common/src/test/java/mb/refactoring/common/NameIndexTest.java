package mb.refactoring.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class NameIndexTest {

	@Test
	void testEqual() {
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(1, "foo");
		assertEquals(index1, index2);
	}

	@Test
	void testNotEqualNumIndex() {
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(2, "foo");
		assertNotEquals(index1, index2);
	}

	@Test
	void testNotEqualPath() {
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(1, "bar");
		assertNotEquals(index1, index2);
	}

	@Test
	void testNotEqualType() {
		NameIndex index = new NameIndex(1, "foo");
		String otherType = "foo";
		assertNotEquals(index, otherType);
	}

	@Test
	void testInvalidNumIndex() {
		 assertThrows(IllegalArgumentException.class, () -> {
			 new NameIndex(-1, "foo");
			  });

	}

	@Test
	void testInvalidPath() {
		 assertThrows(IllegalArgumentException.class, () -> {
			 new NameIndex(1, null);
			  });

	}

	@Test
	void testHashCode() {
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(1, "foo");
		assertEquals(index1.hashCode(), index2.hashCode());
	}

	@Test
	void testToString() {
		NameIndex index = new NameIndex(1, "foo");
		assertEquals(index.toString(), "NameIndex(foo, 1)");
	}
}
