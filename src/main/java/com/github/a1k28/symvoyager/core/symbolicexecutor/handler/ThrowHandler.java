package com.github.a1k28.symvoyager.core.symbolicexecutor.handler;

import com.github.a1k28.symvoyager.core.symbolicexecutor.model.HandlerNode;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SNode;
import sootup.core.jimple.common.stmt.JThrowStmt;

import java.util.List;
import java.util.Optional;

public class ThrowHandler extends AbstractSymbolicHandler {

    public ThrowHandler(SymbolicHandlerContext hc) {
        super(hc);
    }

    @Override
    public SType handle(SMethodPath methodPath, SNode node) throws ClassNotFoundException {
        Class<?> exceptionType = methodPath.getSymbolicVarStack()
                .get(hc.getZ3t().getValueName(((JThrowStmt) node.getUnit()).getOp())).orElseThrow()
                .getClassType();
        Optional<HandlerNode> handlerNode = methodPath.findHandlerNode(node, exceptionType);
        if (handlerNode.isPresent()) {
            hc.getSe().push(methodPath);
            List<SNode> children = handlerNode.get().getNode().getChildren();
            assert children.size() == 1;
            children.get(0).print(1);
            hc.getSe().analyzePaths(handlerNode.get().getMethodPath(), children.get(0));
            hc.getSe().pop(methodPath);
            return SType.THROW;
        } else {
            return SType.THROW_END;
        }
    }
}
