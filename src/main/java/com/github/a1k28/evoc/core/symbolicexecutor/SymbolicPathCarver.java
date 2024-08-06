package com.github.a1k28.evoc.core.symbolicexecutor;

import com.github.a1k28.evoc.core.symbolicexecutor.model.*;
import com.github.a1k28.evoc.core.symbolicexecutor.struct.*;
import com.github.a1k28.evoc.core.z3extended.Z3Translator;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.evoc.helper.SootHelper;
import com.microsoft.z3.*;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.*;
import sootup.core.model.Body;
import sootup.core.model.SootClass;
import sootup.core.model.SootClassMember;
import sootup.core.model.SootMethod;
import sootup.java.core.JavaSootClassSource;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.a1k28.evoc.core.z3extended.Z3Translator.close;
import static com.github.a1k28.evoc.core.z3extended.Z3Translator.makeSolver;
import static com.github.a1k28.evoc.helper.SootHelper.*;

public class SymbolicPathCarver {
    private static final Logger log = Logger.getInstance(SymbolicPathCarver.class);
    private static Solver solver = null;
    private final SStack symbolicVarStack = new SStack();
    private final Z3Translator z3t;
    private final SMethodPath sMethodPath;
    private final SatisfiableResults satisfiableResults;

    public SymbolicPathCarver(String classname, String methodName) throws ClassNotFoundException {
        this(classname, methodName, new SParamList());
    }

    public SymbolicPathCarver(String classname, String methodName, SParamList paramList)
            throws ClassNotFoundException {
        this.sMethodPath = createMethodPath(classname, methodName, paramList);
        this.z3t = new Z3Translator(sMethodPath, symbolicVarStack);
        this.satisfiableResults = new SatisfiableResults(sMethodPath.getTotalLines(), new ArrayList<>());
    }

    public SatisfiableResults analyzeSymbolicPaths()
            throws ClassNotFoundException {
        solver = makeSolver();
        analyzePaths(sMethodPath.getRoot());
        return satisfiableResults;
    }

    private SMethodPath createMethodPath(String classname, String methodName, SParamList paramList)
            throws ClassNotFoundException {
        // Find all paths
        SMethodPath sMethodPath = new SMethodPath(classname, methodName, paramList);
        SootClass<JavaSootClassSource> sootClass = getSootClass(sMethodPath.getClassname());

        SootMethod method = getSootMethod(sootClass, methodName);
        Body body = method.getBody();

        List<String> fields = SootHelper.getFields(sootClass).stream()
                .map(SootClassMember::toString).toList();
        sMethodPath.getFields().addAll(fields);

        createFlowDiagram(sMethodPath, body);

        log.debug("Printing method: " + methodName);
        sMethodPath.print();
        return sMethodPath;
    }

    private void analyzePaths(SNode node) throws ClassNotFoundException {
        solver.push();
        symbolicVarStack.push();

        SType type = null;
        satisfiableResults.incrementCoveredLines();

        // handle node types
        if (node.getType() == SType.PARAMETER) {
            if (sMethodPath.getParamList().hasNext())
                saveParameter(node.getUnit(), sMethodPath.getParamList().getNext());
            else
                saveParameter(node.getUnit());
        }
        if (node.getType() == SType.BRANCH_TRUE || node.getType() == SType.BRANCH_FALSE) {
            handleBranch(node);
        }
        if (node.getType() == SType.ASSIGNMENT) {
            type = handleAssignment(node);
        }
        if (node.getType() == SType.INVOKE) {
            type = handleVoidMethodCall(node);
        }

        solver.push();
        if (Z3Status.SATISFIABLE == checkSatisfiability(node, type))
            for (SNode child : node.getChildren())
                analyzePaths(child);
        solver.pop();

        solver.pop();
        symbolicVarStack.pop();
    }

    private void handleBranch(SNode node) {
        JIfStmt ifStmt = (JIfStmt) node.getUnit();
        Value condition = ifStmt.getCondition();
        Expr z3Condition = z3t.translateCondition(condition, getVarType(ifStmt));
        solver.add(z3t.mkEq(z3Condition, node.getType() == SType.BRANCH_TRUE));
    }

    private SType handleAssignment(SNode node)
            throws ClassNotFoundException {
        SatisfiableResults response = handleAssignmentAndPropagate(node);
        if (response == null) return SType.ASSIGNMENT;
        updateStackAndPropagate(node, response.getResults());
        if (!response.getResults().isEmpty()) return SType.INVOKE;
        return SType.ASSIGNMENT;
    }

    private SType handleVoidMethodCall(SNode node)
            throws ClassNotFoundException {
        JInvokeStmt invoke = (JInvokeStmt) node.getUnit();
        SExpr wrapped = z3t.wrapMethodCall(invoke.getInvokeExpr());
        if (wrapped.getSType() != SType.INVOKE) return SType.OTHER;
        SMethodExpr method = wrapped.asMethod();
        if (!method.isInvokable()) {
            z3t.callProverMethod(method, getVarType(invoke));
            return SType.OTHER;
        }
        SatisfiableResults response = returnPermutations(method);
        updateStackAndPropagate(node, response.getResults());
        return SType.INVOKE;
    }

    private SatisfiableResults handleAssignmentAndPropagate(SNode node) throws ClassNotFoundException {
        JAssignStmt assignStmt = (JAssignStmt) node.getUnit();
        Value leftOp = assignStmt.getLeftOp();
        Value rightOp = assignStmt.getRightOp();
        VarType leftOpVarType = getVarType(leftOp);
        VarType rightOpVarType = getVarType(rightOp);
        SExpr holder = z3t.translateAndWrapValue(rightOp, rightOpVarType);

        if (holder.getSType() == SType.INVOKE) {
            SMethodExpr methodExpr = holder.asMethod();
            if (methodExpr.isInvokable()) {
                return returnPermutations(methodExpr);
            } else {
                Expr expr = z3t.callProverMethod(holder.asMethod(), rightOpVarType);
                z3t.updateSymbolicVariable(leftOp, expr, leftOpVarType);
            }
        } else {
            // if method cannot be invoked, then mock it.
            if (rightOpVarType == VarType.METHOD) leftOpVarType = VarType.METHOD_MOCK;
            z3t.updateSymbolicVariable(leftOp, holder.getExpr(), leftOpVarType);
        }
        return null;
    }

    private List<SVar> handleReturn(SNode node) {
        List<SVar> vars = new ArrayList<>();
        vars.add(copyReturnValue(node));
        vars.addAll(symbolicVarStack.getFields());
        keepLastOccurrence(vars);
        return vars;
    }

    private List<SVar> handleVoidReturn() {
        List<SVar> vars = new ArrayList<>(symbolicVarStack.getFields());
        keepLastOccurrence(vars);
        return vars;
    }

    private void updateStackAndPropagate(SNode node, List<SatisfiableResult> satisfiableResults)
            throws ClassNotFoundException {
        Value leftOp = null;
        if (node.getUnit() instanceof JAssignStmt<?,?> assignStmt) {
            leftOp = assignStmt.getLeftOp();
        }
        VarType varType = getVarType(node.getUnit());

        for (SatisfiableResult result : satisfiableResults) {
            solver.push();
            symbolicVarStack.push();

            // add fields
            for (SVar field : result.getFields())
                z3t.updateSymbolicVariable(field.getValue(), field.getExpr(), VarType.FIELD);

            // add return val
            if (leftOp != null)
                z3t.updateSymbolicVariable(leftOp, result.getReturnValue().getExpr(), varType);

            // add assertions
            Set<BoolExpr> existingAssertions = Set.of(solver.getAssertions());
            for (BoolExpr assertion : result.getZ3Assertions())
                if (!existingAssertions.contains(assertion))
                    solver.add(assertion);

            // propagate
            for (SNode child : node.getChildren())
                analyzePaths(child);

            solver.pop();
            symbolicVarStack.pop();
        }
    }

    private void keepLastOccurrence(List<SVar> vars) {
        Set<String> visited = new HashSet<>();
        for (int i = vars.size()-1; i>=0; i--) {
            if (visited.contains(vars.get(i).getName())) {
                vars.remove(i);
            } else {
                visited.add(vars.get(i).getName());
            }
        }
    }

    private SatisfiableResults returnPermutations(SMethodExpr methodExpr) throws ClassNotFoundException {
        String sig = methodExpr.getInvokeExpr().getMethodSignature().getSubSignature().toString();
        List<Value> args = methodExpr.getArgs();
        args.addAll(symbolicVarStack.getFields().stream()
                .map(SVar::getValue)
                .toList());
        List<Expr> exprArgs = args.stream()
                .map(e -> z3t.translateValue(e, getVarType(e)))
                .collect(Collectors.toList());
        return new SymbolicPathCarver(sMethodPath.getClassname(), sig, new SParamList(exprArgs))
                .analyzeSymbolicPaths();
    }

    private Z3Status checkSatisfiability(SNode node, SType type) {
        if (solver.check() != Status.SATISFIABLE) {
            log.warn("Path is unsatisfiable\n");
            return Z3Status.UNSATISFIABLE_END;
        } else if (SType.INVOKE != type) {
            if (node.getChildren().isEmpty()) {
                // if tail
                SVar returnValue = null;
                if (node.getType() == SType.RETURN) {
                    JReturnStmt stmt = (JReturnStmt) node.getUnit();
                    Expr expr = z3t.translateValue(stmt.getOp(), VarType.RETURN_VALUE);
                    returnValue = new SVar(z3t.getValueName(stmt.getOp()),
                            stmt.getOp(), expr, VarType.RETURN_VALUE, true);
                }
                handleSatisfiability(returnValue);
                return Z3Status.SATISFIABLE_END;
            }
        }
        if (SType.INVOKE == type) return Z3Status.SATISFIABLE_END;
        return Z3Status.SATISFIABLE;
    }

    private void handleSatisfiability(SVar returnValue) {
        log.info("Path is satisfiable");
        Model model = solver.getModel();

        Map<SVar, String> res = new LinkedHashMap<>();
        for (SVar var : symbolicVarStack.getAll()) {
            if (!var.isDeclaration()) continue;
            if (var.getType() != VarType.PARAMETER
                    && var.getType() != VarType.FIELD
                    && var.getType() != VarType.METHOD_MOCK) continue;
            Object evaluated = model.eval(var.getExpr(), true);
            res.put(var, evaluated.toString());
            log.debug(evaluated + " " + var);
        }

        // TODO: handle throw clause
        SatisfiableResult satisfiableResult = new SatisfiableResult(
                solver.getAssertions(),
                symbolicVarStack.getFields(),
                returnValue,
                true,
                res
        );

        satisfiableResults.getResults().add(satisfiableResult);
        log.empty();
    }

    private void saveParameter(Stmt unit) {
        Local ref = ((JIdentityStmt<?>) unit).getLeftOp();
        z3t.saveSymbolicVar(ref, ref.getType(), getVarType(unit));
    }

    private void saveParameter(Stmt unit, Expr expr) {
        Local ref = ((JIdentityStmt<?>) unit).getLeftOp();
        z3t.updateSymbolicVariable(ref, expr, getVarType(unit));
//        z3t.saveSymbolicVar(ref, ref.getType(), getVarType(unit));
    }

    private SVar copyReturnValue(SNode node) {
        JReturnStmt unit = (JReturnStmt) node.getUnit();
        String name = z3t.getValueName(unit.getOp());
        Optional<SVar> optional = symbolicVarStack.get(name);
        if (optional.isPresent()) return new SVar(optional.get(), VarType.RETURN_VALUE);
        Expr expr = z3t.translateValue(unit.getOp(), VarType.RETURN_VALUE);
        return new SVar(name, unit.getOp(), expr, VarType.RETURN_VALUE, true);
    }

    private VarType getVarType(Stmt unit) {
        String val = unit.toString();
        if (val.startsWith("this.")) val = val.substring(5);
        if (sMethodPath.getFields().contains(val)) return VarType.FIELD;
        return VarType.getType(unit);
    }

    private VarType getVarType(Value value) {
        String val = value.toString();
        if (val.startsWith("this.")) val = val.substring(5);
        if (sMethodPath.getFields().contains(val)) return VarType.FIELD;
        return VarType.getType(value);
    }

    public static void main(String[] args) throws ClassNotFoundException {
        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3.dylib");
//        System.load("/Users/ak/Desktop/z3-4.13.0-arm64-osx-11.0/bin/libz3java.dylib");
//        new SymbolicPathCarver("com.github.a1k28.Stack", "test_method_call")
//                .analyzeSymbolicPaths();

        Z3Translator.initZ3();

        Context ctx = new Context();
        ArrayExpr array = ctx.mkArrayConst("arr", ctx.getIntSort(), ctx.getIntSort());

// Create a variable to be "removed"
        IntExpr variableToRemove = ctx.mkInt(400);
        IntExpr varrr = ctx.mkIntConst("x");

// Store some constants in the array
        array = ctx.mkStore(array, ctx.mkInt(0), ctx.mkInt(42));
        array = ctx.mkStore(array, ctx.mkInt(1), varrr);
        array = ctx.mkStore(array, ctx.mkInt(2), ctx.mkInt(7));
        array = ctx.mkStore(array, ctx.mkInt(3), ctx.mkInt(142));

// Assume the original size is 3
//        IntExpr originalSize = ctx.mkInt(3);

// Create a new array with the variable "removed"
        Expr<IntSort> sentinel = ctx.mkConst("null", ctx.mkIntSort()); // Use -1 as a sentinel value
//        Expr<IntSort> sentinel = ctx.mkInt("99999999999999999999");
//        Expr<IntSort> sentinel = ctx.mkInt(-1); // Use -1 as a sentinel value

// Create a function to "remove" the variable from each index
        BoolExpr condition = ctx.mkEq(variableToRemove, ctx.mkSelect(array, ctx.mkIntConst("i")));
        Expr<IntSort> thenExpr = sentinel;
        Expr<IntSort> elseExpr = ctx.mkSelect(array, ctx.mkIntConst("i"));
        Expr<IntSort> removalFunction = ctx.mkITE(condition, thenExpr, elseExpr);

// Apply the removal function to the entire array
        ArrayExpr newArray = ctx.mkLambda(new Expr[]{ctx.mkIntConst("i")}, removalFunction);

        ArithExpr newSize = ctx.mkInt(0);
        ArithExpr originalSize = ctx.mkInt(4);
        for (int i = 0; i < 4; i++) {
            Expr condition1 = ctx.mkEq(ctx.mkSelect(newArray, ctx.mkInt(i)), sentinel);
            Expr thenExpr1 = ctx.mkInt(0);
            Expr elseExpr1 = ctx.mkInt(1);
            newSize = ctx.mkAdd(newSize, ctx.mkITE(condition1, thenExpr1, elseExpr1));

//            newSize = ctx.mkAdd(newSize, ctx.mkITE(ctx.mkEq(ctx.mkSelect(newArray, ctx.mkInt(i)), defaultValue), ctx.mkInt(0), ctx.mkInt(1)));
//            originalSize = ctx.mkAdd(originalSize, ctx.mkITE(ctx.mkEq(ctx.mkSelect(newArray, ctx.mkInt(i)), defaultValue), ctx.mkInt(1), ctx.mkInt(1)));
        }

// Create a function to count non-default values
//        FuncDecl<IntSort> countFunc = ctx.mkFuncDecl("countFunc", ctx.getIntSort(), ctx.getIntSort());
//        ctx.mkForall(
//                new Expr[]{ctx.mkIntConst("i")},
//                ctx.mkEq(
//                        ctx.mkApp(countFunc, ctx.mkIntConst("i")),
//                        ctx.mkITE(
//                                ctx.mkEq(ctx.mkSelect(newArray, ctx.mkIntConst("i")), defaultValue),
//                                ctx.mkInt(0),
//                                ctx.mkInt(1)
//                        )
//                ),
//                1,
//                null,
//                null,
//                null,
//                null
//        );
//
//// Sum up the count for all indices up to the original size
//        for (int i = 0; i < 3; i++) {
//            newSize = ctx.mkAdd(newSize, (ArithExpr) ctx.mkApp(countFunc, ctx.mkInt(i)));
//        }

// Now newSize represents the size of the new array after removal

// We can use the solver to reason about the new size
        Solver solver = ctx.mkSolver();

// Check if the new size is less than the original size
        BoolExpr sizeReduced = ctx.mkLt(newSize, originalSize);
//        solver.push();
        solver.add(ctx.mkEq(sizeReduced, ctx.mkBool(true)));

//        if (solver.check() == Status.SATISFIABLE) {
//            System.out.println("The size of the array may have been reduced");
//            Model model = solver.getModel();
//            System.out.println("Possible new size: " + model.eval(newSize, true));
//            System.out.println("Possible value of x that was removed: " + model.eval(varrr, true));
//        } else {
//            System.out.println("The size of the array was not reduced");
//        }

// We can also check for specific size values
        solver.push();
//        solver.add(ctx.mkEq(newSize, ctx.mkInt(2)));

        if (solver.check() == Status.SATISFIABLE) {
            Model model = solver.getModel();
            System.out.println("The new size could be: " + model.eval(newSize, true));
            System.out.println("A value of x that makes this true: " + model.eval(varrr, true));
            System.out.println("A value of sentinel that makes this true: " + model.eval(sentinel, true));
        }
        solver.pop();

        close();
    }
}