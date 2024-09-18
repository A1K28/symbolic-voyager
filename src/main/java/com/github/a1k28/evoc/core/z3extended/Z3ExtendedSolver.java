package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.core.z3extended.model.SortType;
import com.github.a1k28.evoc.core.z3extended.model.Tuple;
import com.github.a1k28.evoc.helper.Logger;
import com.microsoft.z3.*;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class Z3ExtendedSolver {
    private static final Logger log = Logger.getInstance(Z3ExtendedSolver.class);

    private final Z3ExtendedContext ctx;
    private final Solver solver;
    private final Z3SortUnion sortUnion;

    public void push() {
        solver.push();
    }

    public void pop() {
        solver.pop();
    }

    public void add(Expr... vars) {
        solver.add(vars);
    }

    public Status check() {
        return solver.check();
    }

    public BoolExpr[] getAssertions() {
        return solver.getAssertions();
    }

    public Model getModel() {
        return solver.getModel();
    }

    // debug method
    public boolean isSatisfiable(BoolExpr x) {
        return getSatisfiableStatus(x) == Status.SATISFIABLE;
    }

    public boolean isUnsatisfiable(BoolExpr x) {
        return getSatisfiableStatus(x) == Status.UNSATISFIABLE;
    }

    // TODO: fix this
    public <T> T deserialize(Expr expr) {
        if (SortType.NULL.equals(expr.getSort()))
            return (T) "null";
        return (T) expr.toString();
    }

    public int minimizeInteger(Expr x) {
        // Binary search for the minimum value of x
        int low = 0;
        int high = Integer.MAX_VALUE;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) / 2;
            solver.push();
            add(ctx.mkLe(x, ctx.mkInt(mid)));

            if (solver.check() == Status.SATISFIABLE) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
            solver.pop();
        }

        // TODO: verify for unintended side-effects
        add(ctx.mkEq(x, ctx.mkInt(result)));

        return result;
    }

    public Map createInitialMap(MapModel mapModel, int size) {
        Map target = new HashMap<>();
        Queue<Expr> discoveredValues = new LinkedList<>();
        for (Tuple<Expr> tuple : mapModel.getDiscoveredKeys()) {
            Expr key = tuple.getO1();
            Expr keyWrapped = tuple.getO2();
            BoolExpr condition = ctx.mkNot(ctx.mkMapContainsWrappedKey(mapModel, keyWrapped));

            // only continue if the condition is UNSATISFIABLE
            if (!isUnsatisfiable(condition)) {
                add(condition);
                continue;
            };

            Expr retrieved = ctx.mkSelect(mapModel.getArray(), keyWrapped);
            if (key == null) {
                discoveredValues.add(retrieved);
                continue;
            }
//            key = sortUnion.unwrapValue(keyWrapped).get();
//            Optional<Expr> keyOpt = sortUnion.unwrapValue(keyWrapped);
//            if (keyOpt.isEmpty()) {
//                discoveredValues.add(retrieved);
//                continue;
//            }

            solver.check(); // mandatory check before calling getModel()
            Model model = solver.getModel();

            key = model.eval(key, true);
            Expr valueWrapped = mapModel.getValue(retrieved);
            Expr unwrappedValue = sortUnion.unwrapValue(valueWrapped)
                    .orElseGet(Z3Translator::mkNull);
            Expr value = model.eval(unwrappedValue, true);
            target.put(deserialize(key), deserialize(value));
            log.debug("(filled) Key:Value " + key + ":" + value);

            // update solver state
            add(ctx.mkMapExistsByKeyAndValueCondition(mapModel, retrieved, keyWrapped, valueWrapped));
        }

        if (target.size() < size)
            fillUnknownMapKeys(target, mapModel, size-target.size(), discoveredValues);

        return target;
    }

    private void fillUnknownMapKeys(
            Map target, MapModel mapModel, int size, Queue<Expr> discoveredValues) {
        ArrayExpr map = mapModel.getArray();

        // Create a symbolic key
        Expr symbolicKey = ctx.mkConst("symbolicKey", mapModel.getKeySort());

        // Create a constraint: there exists a key where map[key] == targetValue
        Expr<BoolSort>[] boolExprs = new BoolExpr[mapModel.getDiscoveredKeys().size()];
        for (int i = 0; i < mapModel.getDiscoveredKeys().size(); i++) {
//            Expr key = mapModel.getDiscoveredKeysTuple().get(i).getO1();
            Expr keyWrapped = mapModel.getDiscoveredKeys().get(i).getO2();

            BoolExpr c = ctx.mkNot(ctx.mkEq(mapModel.getKey(ctx.mkSelect(map, symbolicKey)), keyWrapped));
            boolExprs[i] = c;
        }

        BoolExpr condition = ctx.mkAnd(
                ctx.mkNot(mapModel.isEmpty(ctx.mkSelect(map, symbolicKey))),
                ctx.mkEq(mapModel.getKey(ctx.mkSelect(map, symbolicKey)), symbolicKey),
                ctx.mkAnd(boolExprs)
        );

        BoolExpr existsMatch = ctx.mkExists(
                new Expr[]{symbolicKey},
                condition,
                1, null, null, null, null
        );

        // Add the constraint to the solver
        solver.push();
        add(existsMatch);
        add(ctx.mkImplies(existsMatch, condition));

        int i = 0;
        while (solver.check() == Status.SATISFIABLE && i < size) {
            i++;
            Model model = solver.getModel();

            // assuming that all keys are unique across different maps
            String uuid = UUID.randomUUID().toString();
            uuid = uuid.substring(uuid.lastIndexOf("-")+1);
            Expr key = ctx.mkString(uuid); // TODO: string??
            Expr keyWrapped = sortUnion.wrapValue(key);

            Expr retrieved;
            if (discoveredValues.isEmpty()) {
                retrieved = ctx.mkSelect(mapModel.getArray(), keyWrapped);
            } else {
                System.out.println("HEREEE");
                retrieved = discoveredValues.poll();
            }

            Expr valueWrapped = mapModel.getValue(retrieved);
            Expr unwrappedValue = sortUnion.unwrapValue(valueWrapped)
                    .orElseGet(Z3Translator::mkNull);
            Expr value = model.eval(unwrappedValue, true);
            target.put(deserialize(key), deserialize(value));
            log.debug("(filled unknown) Key:Value " + key + ":" + value);

            // Add a constraint to exclude this key in the next iteration
            add(ctx.mkNot(ctx.mkEq(symbolicKey, keyWrapped)));
            add(ctx.mkMapExistsByKeyAndValueCondition(mapModel, retrieved, keyWrapped, valueWrapped));
        }
        solver.pop();
    }

    private Status getSatisfiableStatus(BoolExpr x) {
        solver.push();
        add(x);
        Status status = solver.check();
        solver.pop();
        if (status == Status.UNKNOWN)
            log.warn("UNKNOWN status for expression: " + x);
        return status;
    }
}
