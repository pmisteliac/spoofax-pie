package mb.refactoring.common;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ResolutionPairTest {

	@Test
	void testEqual() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);
		ResolutionPair pair2 = new ResolutionPair("foo", 2, 1);

		assertEquals(pair1, pair2);
	}

	@Test
	void testNotEqualRef() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);
		ResolutionPair pair2 = new ResolutionPair("foo", 3, 1);

		assertNotEquals(pair1, pair2);
	}

	@Test
	void testNotEqualDec() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);
		ResolutionPair pair2 = new ResolutionPair("foo", 2 ,3);

		assertNotEquals(pair1, pair2);
	}

	@Test
	void testNotEqualType() {
		ResolutionPair pair = new ResolutionPair("foo", 2, 1);

		String otherType = "foo";
		assertNotEquals(pair, otherType);
	}

	@Test
	void testInvalidRef() {
		 assertThrows(IllegalArgumentException.class, () -> {
				NameIndex index = new NameIndex(1, "foo");
				new ResolutionPair(null, index);
			  });

	}

	@Test
	void testInvalidDec() {
		 assertThrows(IllegalArgumentException.class, () -> {
				NameIndex index = new NameIndex(1, "foo");
				new ResolutionPair(index, null);
			  });

	}


	@Test
	void testConstructorOne() {
		NameIndex ref = new NameIndex(2, "foo");
		NameIndex dec = new NameIndex(1, "foo");
		ResolutionPair pair = new ResolutionPair(ref, dec);

		assertEquals(ref, pair.getReference());
		assertEquals(dec, pair.getDeclaration());
	}

	@Test
	void testConstructorTwo() {
		NameIndex ref = new NameIndex(2, "foo");
		NameIndex dec = new NameIndex(1, "foo");
		ResolutionPair pair = new ResolutionPair("foo", 2, 1);

		assertEquals(ref, pair.getReference());
		assertEquals(dec, pair.getDeclaration());
	}

	@Test
	void testHashCode() {
		ResolutionPair pair1 = new ResolutionPair("foo", 2, 1);
		ResolutionPair pair2 = new ResolutionPair("foo", 2, 1);

		assertEquals(pair1.hashCode(), pair2.hashCode());

	}

	@Test
	void testToString() {
		NameIndex ref = new NameIndex(2, "foo");
		NameIndex dec = new NameIndex(1, "foo");
		ResolutionPair pair = new ResolutionPair(ref, dec);

		assertEquals(pair.toString(), "ResolutionPair(NameIndex(foo, 2), NameIndex(foo, 1))");
	}

}
