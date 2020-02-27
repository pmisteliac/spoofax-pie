package mb.statix.common;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import io.usethesource.capsule.Map;
import io.usethesource.capsule.Set;
import mb.nabl2.terms.ITerm;
import mb.nabl2.terms.ITermVar;
import mb.nabl2.terms.unification.UnifierFormatter;
import mb.nabl2.terms.unification.Unifiers;
import mb.nabl2.terms.unification.ud.IUniDisunifier;
import mb.nabl2.util.CapsuleUtil;
import mb.statix.constraints.messages.IMessage;
import mb.statix.constraints.messages.MessageKind;
import mb.statix.scopegraph.reference.CriticalEdge;
import mb.statix.solver.Delay;
import mb.statix.solver.IConstraint;
import mb.statix.solver.IState;
import mb.statix.solver.completeness.Completeness;
import mb.statix.solver.completeness.ICompleteness;
import mb.statix.solver.persistent.SolverResult;
import mb.statix.spec.ApplyResult;
import mb.statix.spec.Spec;
import org.metaborg.util.functions.Function2;
import org.metaborg.util.log.ILogger;
import org.metaborg.util.log.LoggerUtils;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;


/**
 * The state of the solver.
 */
public final class SolverState {

    private final static ILogger log = LoggerUtils.logger(SolverState.class);

    /**
     * Creates a new {@link SolverState} from the given specification, solver state, and constraints.
     *
     * @param spec the Statix specification
     * @param state the solver state
     * @param constraints the constraints
     * @return the resulting search state
     */
    public static SolverState of(Spec spec, IState.Immutable state, Iterable<? extends IConstraint> constraints) {
        final ICompleteness.Transient completeness = Completeness.Transient.of(spec);
        completeness.addAll(constraints, state.unifier());

        return new SolverState(state, Map.Immutable.of(), CapsuleUtil.toSet(constraints), Map.Immutable.of(),
                null, completeness.freeze());
    }

    /**
     * Creates a new {@link SolverState} from the given solver result.
     *
     * @param result the result of inference by the solver
     * @param existentials
     * @return the resulting search state
     */
    public static SolverState fromSolverResult(SolverResult result, @Nullable ImmutableMap<ITermVar, ITermVar> existentials) {
        final Set.Transient<IConstraint> constraints = Set.Transient.of();
        final Map.Transient<IConstraint, Delay> delays = Map.Transient.of();
        result.delays().forEach((c, d) -> {
            if(d.criticalEdges().isEmpty()) {
                constraints.__insert(c);
            } else {
                delays.__put(c, d);
            }
        });

        final ImmutableMap<ITermVar, ITermVar> newExistentials =
                existentials == null ? result.existentials() : existentials;
        return new SolverState(result.state(), CapsuleUtil.toMap(result.messages()), constraints.freeze(), delays.freeze(), newExistentials,
                result.completeness());
    }

    private final IState.Immutable state;
    private final Set.Immutable<IConstraint> constraints;
    private final Map.Immutable<IConstraint, Delay> delays;
    @Nullable private final ImmutableMap<ITermVar, ITermVar> existentials;
    private final ICompleteness.Immutable completeness;
    private final Map.Immutable<IConstraint, IMessage> messages;

    protected SolverState(IState.Immutable state,
                          Map.Immutable<IConstraint, IMessage> messages,
                          Set.Immutable<IConstraint> constraints,
                          Map.Immutable<IConstraint, Delay> delays,
                          @Nullable ImmutableMap<ITermVar, ITermVar> existentials,
                          ICompleteness.Immutable completeness) {
        this.state = state;
        this.messages = messages;
        this.constraints = constraints;
        this.delays = delays;
        this.existentials = existentials;
        this.completeness = completeness;
    }

    /** Gets the solver state. */
    public IState.Immutable getState() {
        return state;
    }

    /** Gets the messages. */
    public Map.Immutable<IConstraint, IMessage> getMessages() {
        return this.messages;
    }

    /** Determines whether any of the messages in this state are error messages. */
    public boolean hasErrors() {
        return this.messages.values().stream().anyMatch(m -> m.kind().equals(MessageKind.ERROR));
    }

    /** The constraints left to solve. */
    public Set.Immutable<IConstraint> getConstraints() {
        return constraints;
    }

    /** The constraints that have been delayed due to critical edges. */
    public Map.Immutable<IConstraint, Delay> getDelays() {
        return delays;
    }

    /**
     * The variables that have been existentially quantified in the most top-level constraint;
     * or {@code null} when no constraints have existentially quantified any variables (yet).
     *
     * This is used to be able to find the value assigned to the top-most quantified variables.
     */
    @Nullable public ImmutableMap<ITermVar, ITermVar> getExistentials() {
        return this.existentials;
    }

    public ICompleteness.Immutable getCompleteness() {
        return completeness;
    }

    /**
     * Updates this search state with the specified {@link ApplyResult} and returns the new state.
     *
     * @param result the {@link ApplyResult}
     * @param focus the focus constraint
     * @return the updated search state
     */
    public SolverState withApplyResult(ApplyResult result, IConstraint focus) {
        final IConstraint applyConstraint = result.body();
        final IState.Immutable applyState = result.state();
        final IUniDisunifier.Immutable applyUnifier = applyState.unifier();

        // Update constraints
        final Set.Transient<IConstraint> constraints = this.getConstraints().asTransient();
        constraints.__insert(applyConstraint);
        constraints.__remove(focus);

        // Update completeness
        final ICompleteness.Transient completeness = this.getCompleteness().melt();
        completeness.updateAll(result.diff().varSet(), applyUnifier);
        completeness.add(applyConstraint, applyUnifier);
        java.util.Set<CriticalEdge> removedEdges = completeness.remove(focus, applyUnifier);

        // Update delays
        final Map.Transient<IConstraint, Delay> delays = Map.Transient.of();
        this.getDelays().forEach((c, d) -> {
            if(!Sets.intersection(d.criticalEdges(), removedEdges).isEmpty()) {
                constraints.__insert(c);
            } else {
                delays.__put(c, d);
            }
        });

        return new SolverState(applyState, messages, constraints.freeze(), delays.freeze(), existentials, completeness.freeze());
    }

    /**
     * Update the constraints, keeping completeness and delayed constraints in sync.
     *
     * This method assumes that no constraints appear in both add and remove, or it will be incorrect!
     *
     * @param add the constraints to add
     * @param remove the constraints to remove
     * @return the new search state
     */
    public SolverState updateConstraints(Iterable<IConstraint> add, Iterable<IConstraint> remove) {

        final ICompleteness.Transient completeness = this.completeness.melt();
        final Set.Transient<IConstraint> constraints = this.constraints.asTransient();
        final java.util.Set<CriticalEdge> removedEdges = Sets.newHashSet();
        add.forEach(c -> {
            if(constraints.__insert(c)) {
                completeness.add(c, state.unifier());
            }
        });
        remove.forEach(c -> {
            if(constraints.__remove(c)) {
                removedEdges.addAll(completeness.remove(c, state.unifier()));
            }
        });
        final Map.Transient<IConstraint, Delay> delays = Map.Transient.of();
        this.delays.forEach((c, d) -> {
            if(!Sets.intersection(d.criticalEdges(), removedEdges).isEmpty()) {
                constraints.__insert(c);
            } else {
                delays.__put(c, d);
            }
        });
        return new SolverState(state, messages, constraints.freeze(), delays.freeze(), existentials, completeness.freeze());
    }

    public SolverState delay(Iterable<? extends java.util.Map.Entry<IConstraint, Delay>> delay) {
        final Set.Transient<IConstraint> constraints = this.constraints.asTransient();
        final Map.Transient<IConstraint, Delay> delays = this.delays.asTransient();
        delay.forEach(entry -> {
            if(constraints.__remove(entry.getKey())) {
                delays.__put(entry.getKey(), entry.getValue());
            } else {
                log.warn("delayed constraint not in constraint set: {}", entry.getKey());
            }
        });
        return new SolverState(state, messages, constraints.freeze(), delays.freeze(), existentials, completeness);
    }

    @Override public String toString() {
        StringWriter out = new StringWriter();
        PrintWriter writer = new PrintWriter(out);
        try {
            writer.println("SolverState:");
            write(writer, (t, u) -> new UnifierFormatter(u, 2).format(t));
        } catch (IOException e) {
            // This can never happen.
            throw new RuntimeException(e);
        }
        return out.toString();
    }

    public void write(PrintWriter writer, Function2<ITerm, IUniDisunifier, String> prettyprinter) throws IOException {
        final IUniDisunifier unifier = state.unifier();
        if (existentials != null) {
            writer.println("| vars:");
            for (Map.Entry<ITermVar, ITermVar> existential : existentials.entrySet()) {
                String var = prettyprinter.apply(existential.getKey(), Unifiers.Immutable.of());
                String term = prettyprinter.apply(existential.getValue(), unifier);
                writer.println("|   " + var + " : " + term);
            }
        } else {
            writer.println("| vars: <null>");
        }
        writer.println("| unifier: " + state.unifier().toString());
        writer.println("| completeness: " + completeness.toString());
        writer.println("| constraints:");
        for (IConstraint c : constraints) {
            writer.println("|   " + c.toString(t -> prettyprinter.apply(t, unifier)));
        }
        writer.println("| delays:");
        for (java.util.Map.Entry<IConstraint, Delay> e : delays.entrySet()) {
            writer.println("|   " + e.getValue() + " : " + e.getKey().toString(t -> prettyprinter.apply(t, unifier)));
        }
        writer.println("| messages:");
        for (java.util.Map.Entry<IConstraint, IMessage> e : messages.entrySet()) {
            writer.println("|   - " + e.getValue().toString(ITerm::toString));
            writer.println("|     " + e.getKey().toString(t -> prettyprinter.apply(t, unifier)));
        }
    }
}
