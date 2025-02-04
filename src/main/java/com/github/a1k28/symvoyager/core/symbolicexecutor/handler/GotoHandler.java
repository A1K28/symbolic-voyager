package com.github.a1k28.symvoyager.core.symbolicexecutor.handler;

import com.github.a1k28.symvoyager.core.cli.model.CLIOptions;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.SType;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SMethodPath;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SNode;

import java.util.List;

public class GotoHandler extends AbstractSymbolicHandler {
    public GotoHandler(SymbolicHandlerContext handlerContext) {
        super(handlerContext);
    }

    @Override
    public SType handle(SMethodPath methodPath, SNode node) throws ClassNotFoundException {
        List<SNode> nodes = methodPath.getTargetNodes(node.getUnit());

        // under-approximate
        for (SNode n : nodes) {
            if (hc.getSe().getDepth() <= CLIOptions.depthLimit) {
                hc.getSe().analyzePaths(methodPath, n);
            }
//            if (methodPath.getClassInstance().incrementGotoCount(node)) {
//            }
        }

        return SType.GOTO;
    }
}
