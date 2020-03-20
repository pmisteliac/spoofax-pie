package mb.refactoring.common;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public final class EquivalenceClassCalculator {

	private EquivalenceClassCalculator() {

	}

	public static void main(String[] args) {

	}

	public static void calculate(Set<ResolutionPair> resolutionRelation) {
		Set<NameIndex> nameIndexes = gatherNameIndexes(resolutionRelation);
		Set<ResolutionPair> reflexiveClosure = calcReflexiveClosure(nameIndexes);
		Set<ResolutionPair> symetricClosure = calcSymetricClosure(resolutionRelation);
		//calculate transitive closure

		Set<ResolutionPair> equivalenceRelation = new HashSet<>();
		equivalenceRelation.addAll(resolutionRelation);
		equivalenceRelation.addAll(reflexiveClosure);
		equivalenceRelation.addAll(symetricClosure);
		//calculate equivalence classes
	}

	public static Set<NameIndex> gatherNameIndexes (Set<ResolutionPair> resolutionRelation) {
		Set<NameIndex> indexes = new HashSet<>();
		for(ResolutionPair pair: resolutionRelation) {
			indexes.add(pair.getDeclaration());
			indexes.add(pair.getReference());
		}
		return indexes;
	}

	public static Set<ResolutionPair> calcReflexiveClosure(Set<NameIndex> nameIndexes) {
		Set<ResolutionPair> closure = nameIndexes.stream()
									  .map(index -> new ResolutionPair(index, index))
									  .collect(Collectors.toSet());
		return closure;

	}

	public static Set<ResolutionPair> calcSymetricClosure(Set<ResolutionPair> resolutionRelation) {
		Set<ResolutionPair> closure = resolutionRelation.stream()
									  .map(pair -> new ResolutionPair(pair.getDeclaration(), pair.getReference()))
									  .collect(Collectors.toSet());
		return closure;
	}

}
