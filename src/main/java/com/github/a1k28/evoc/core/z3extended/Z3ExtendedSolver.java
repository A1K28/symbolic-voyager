package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.helper.Logger;
import com.microsoft.z3.*;
import lombok.RequiredArgsConstructor;

import java.util.*;

@RequiredArgsConstructor
public class Z3ExtendedSolver {
    private static final Logger log = Logger.getInstance(Z3ExtendedSolver.class);

    private final Z3ExtendedContext ctx;
    private final Solver solver;

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

    public int minimizeInteger(Expr x) {
        // Binary search for the minimum value of x
        int low = 0;
        int high = Integer.MAX_VALUE;
        int result = -1;

        while (low <= high) {
            int mid = (low + high) / 2;
            solver.push();
            solver.add(ctx.mkLe(x, ctx.mkInt(mid)));

            if (solver.check() == Status.SATISFIABLE) {
                result = mid;
                high = mid - 1;
            } else {
                low = mid + 1;
            }
            solver.pop();
        }

        // TODO: verify for unintended side-effects
        solver.add(ctx.mkEq(x, ctx.mkInt(result)));

        return result;
    }

    public Map createInitialMap(MapModel mapModel, int size) {
        Map target = new HashMap<>();
        for (int i = 0; i < mapModel.getDiscoveredKeys().size(); i++) {
            Expr key = mapModel.getDiscoveredKeys().get(i);
            BoolExpr condition = ctx.mkNot(ctx.mkMapContainsKey(mapModel, key));

            // only continue if the condition is UNSATISFIABLE
            if (!isUnsatisfiable(condition)) {
                solver.add(condition);
                continue;
            };

            solver.check(); // mandatory check before calling getModel()
            Model model = solver.getModel();

            Expr retrieved = ctx.mkSelect(mapModel.getArray(), key);
            key = model.eval(key, true);
            Expr value = model.eval(mapModel.getValue(retrieved), true);
            target.put(key.toString(), value.toString());
            log.debug("(filled) Key:Value " + key + ":" + value);

            // update solver state
            BoolExpr contains = ctx.mkMapContainsKeyValuePair(mapModel, key, value);
            solver.add(contains);
        }

        if (target.size() < size)
            fillUnknownMapKeys(target, mapModel, size-target.size());

        return target;
    }

    private void fillUnknownMapKeys(Map target, MapModel mapModel, int size) {
        ArrayExpr map = mapModel.getArray();
        SeqSort stringSort = ctx.getStringSort();

        // Create a symbolic key
        Expr<SeqSort> symbolicKey = ctx.mkConst("symbolicKey", stringSort);

        // Create a constraint: there exists a key where map[key] == targetValue
        Expr<BoolSort>[] boolExprs = new BoolExpr[mapModel.getDiscoveredKeys().size()];
        for (int i = 0; i < mapModel.getDiscoveredKeys().size(); i++) {
            Expr unknownKey = mapModel.getDiscoveredKeys().get(i);
            BoolExpr c = ctx.mkNot(ctx.mkEq(mapModel.getKey(ctx.mkSelect(map, symbolicKey)), unknownKey));
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
        solver.add(existsMatch);
        solver.add(ctx.mkImplies(existsMatch, condition));

        int i = 0;
        while (solver.check() == Status.SATISFIABLE && i < size) {
            i++;
            Model model = solver.getModel();

            // Evaluate the symbolicKey in the current model
//            Expr<SeqSort> keyValue = model.eval(symbolicKey, true);

            // assuming that all keys are unique across different maps
            String uuid = UUID.randomUUID().toString();
            uuid = uuid.substring(uuid.lastIndexOf("-")+1);
            Expr key = ctx.mkString(uuid);

            Expr retrieved = ctx.mkSelect(mapModel.getArray(), key);
            Expr value = model.eval(mapModel.getValue(retrieved), true);
            target.put(key.toString(), value.toString());
            log.debug("(filled unknown) Key:Value " + key + ":" + value);

            // Add a constraint to exclude this key in the next iteration
            solver.add(ctx.mkNot(ctx.mkEq(symbolicKey, key)));

            BoolExpr contains = ctx.mkMapContainsKeyValuePair(mapModel, key, value);
            solver.add(contains);
        }
        solver.pop();
    }

    private Status getSatisfiableStatus(BoolExpr x) {
        solver.push();
        solver.add(x);
        Status status = solver.check();
        solver.pop();
        if (status == Status.UNKNOWN)
            log.warn("UNKNOWN status for expression: " + x);
        return status;
    }
}
