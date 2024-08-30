package com.github.a1k28.evoc.core.z3extended;

import com.github.a1k28.evoc.core.z3extended.model.MapModel;
import com.github.a1k28.evoc.helper.Logger;
import com.microsoft.z3.*;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
public class Z3ExtendedSolver {
    private static final Logger log = Logger.getInstance(Z3ExtendedSolver.class);

    private final Context ctx;
    private final Solver solver;

    public void push() {
        solver.push();
    }

    public void pop() {
        solver.pop();
    }

    public void add(Expr expr) {
        solver.add(expr);
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
        return result;
    }

    public Map createInitialMap(MapModel mapModel, int size) {
        Map target = new HashMap<>();
        ArrayExpr map = mapModel.getArray();
        for (int i = 0; i < mapModel.getDiscoveredKeys().size(); i++) {
            Expr keyValue = mapModel.getDiscoveredKeys().get(i);

//            if (i == 3) {
//                solver.push();
//                BoolExpr condition = ctx.mkAnd(
//                        ctx.mkNot(mapModel.isEmpty(ctx.mkSelect(map, keyValue))),
//                        ctx.mkEq(mapModel.getKey(ctx.mkSelect(map, keyValue)), keyValue),
//                        ctx.mkEq(mapModel.getValue(ctx.mkSelect(map, keyValue)), ctx.mkString("VALUE1"))
//                );
//                solver.add(condition);
//                solver.check();
//                solver.pop();
//            }

//            BoolExpr wasInitiallyPresent = mapModel.getKeyExprs().getWasInitiallyPresent().get(i);

            BoolExpr keyEqualityCondition = ctx.mkNot(
                    ctx.mkEq(
                            keyValue,
                            mapModel.getKey(ctx.mkSelect(map, keyValue)))
            );

            solver.push();
            solver.add(keyEqualityCondition);
            Status status = solver.check();
            solver.pop();
            if (status != Status.UNSATISFIABLE) continue;

//            solver.push();
//            solver.add(ctx.mkNot(wasInitiallyPresent));
//            status = solver.check();
//            solver.pop();
//            if (status != Status.UNSATISFIABLE) continue;

            solver.check(); // mandatory check before calling getModel()
            Model model = solver.getModel();

            Expr retrieved = ctx.mkSelect(mapModel.getArray(), keyValue);
            String key = model.eval(keyValue, true).toString();
            String value = model.eval(mapModel.getValue(retrieved), true).toString();
            target.put(key, value);
            log.debug("(filled) Key:Value " + key + ":" + value);
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
            Expr<SeqSort> keyValue = model.eval(symbolicKey, true);

            Expr retrieved = ctx.mkSelect(mapModel.getArray(), keyValue);
            String key = keyValue.toString();
            String value = model.eval(mapModel.getValue(retrieved), true).toString();
            target.put(key, value);
            log.debug("(filled unknown) Key:Value " + key + ":" + value);

            // Add a constraint to exclude this key in the next iteration
            solver.add(ctx.mkNot(ctx.mkEq(symbolicKey, keyValue)));
        }
        solver.pop();
    }
}
