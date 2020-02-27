package mb.statix.common;

import mb.statix.spec.Spec;


/**
 * The context in which the search is performed.
 */
public final class SolverContext {

    private final Spec spec;

    /**
     * Initializes a new instance of the {@link SolverContext} class.
     * @param spec the specification
     */
    public SolverContext(Spec spec) {
        this.spec = spec;
    }

    /** Gets the specification. */
    public Spec getSpec() {
        return spec;
    }

}
