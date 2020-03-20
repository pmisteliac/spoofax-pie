package mb.refactoring.common;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

class EquivalenceClassCalculatorTest {

	@Test
	void testGatherNameIndexesOnePair() {
		NameIndex ref = new NameIndex(2, "foo");
		NameIndex dec = new NameIndex(1, "foo");
		ResolutionPair pair = new ResolutionPair(ref, dec);

		Set<ResolutionPair> resolutionRelation = new HashSet<>();
		resolutionRelation.add(pair);

		Set<NameIndex> indexes = EquivalenceClassCalculator.gatherNameIndexes(resolutionRelation);
		assertEquals(indexes.size(), 2);
		assertTrue(indexes.contains(ref));
		assertTrue(indexes.contains(dec));
	}

	@Test
	void testGatherNameIndexesDuplicate() {
		NameIndex ref1 = new NameIndex(2, "foo");
		NameIndex dec1 = new NameIndex(1, "foo");
		ResolutionPair pair1 = new ResolutionPair(ref1, dec1);

		NameIndex ref2 = new NameIndex(2, "foo");
		NameIndex dec2 = new NameIndex(3, "foo");
		ResolutionPair pair2 = new ResolutionPair(ref2, dec2);

		Set<ResolutionPair> resolutionRelation = new HashSet<>();
		resolutionRelation.add(pair1);
		resolutionRelation.add(pair2);

		Set<NameIndex> indexes = EquivalenceClassCalculator.gatherNameIndexes(resolutionRelation);
		assertEquals(indexes.size(), 3);
		assertTrue(indexes.contains(ref1));
		assertTrue(indexes.contains(dec1));
		assertTrue(indexes.contains(ref2));
		assertTrue(indexes.contains(dec2));
	}

	@Test
	void testCalcReflexiveClosure() {
		Set<NameIndex> indexes = new HashSet<>();
		NameIndex index1 = new NameIndex(1, "foo");
		NameIndex index2 = new NameIndex(2, "foo");
		NameIndex index3 = new NameIndex(3, "foo");

		indexes.add(index1);
		indexes.add(index2);
		indexes.add(index3);

		Set<ResolutionPair> reflexiveClosure = EquivalenceClassCalculator.calcReflexiveClosure(indexes);
		ResolutionPair pair1 = new ResolutionPair(index1, index1);
		ResolutionPair pair2 = new ResolutionPair(index2, index2);
		ResolutionPair pair3 = new ResolutionPair(index3, index3);

		assertEquals(reflexiveClosure.size(), indexes.size());
		assertTrue(reflexiveClosure.contains(pair1));
		assertTrue(reflexiveClosure.contains(pair2));
		assertTrue(reflexiveClosure.contains(pair3));
	}

	@Test
	void testCalcSymetricClosure() {
		NameIndex ref1 = new NameIndex(3, "foo");
		NameIndex dec1 = new NameIndex(1, "foo");
		ResolutionPair pair1 = new ResolutionPair(ref1, dec1);

		NameIndex ref2 = new NameIndex(4, "foo");
		NameIndex dec2 = new NameIndex(2, "foo");
		ResolutionPair pair2 = new ResolutionPair(ref2, dec2);

		Set<ResolutionPair> resolutionRelation = new HashSet<>();
		resolutionRelation.add(pair1);
		resolutionRelation.add(pair2);

		Set<ResolutionPair> closure = EquivalenceClassCalculator.calcSymetricClosure(resolutionRelation);
		ResolutionPair pair1Sym = new ResolutionPair(dec1, ref1);
		ResolutionPair pair2Sym = new ResolutionPair(dec2, ref2);

		assertEquals(closure.size(), resolutionRelation.size());
		assertTrue(closure.contains(pair1Sym));
		assertTrue(closure.contains(pair2Sym));

	}

}
