package com.github.a1k28.evoc.core.mutation;

import com.github.a1k28.evoc.core.mutation.struct.MType;
import com.github.a1k28.evoc.core.symbex.struct.SPath;

import static com.github.a1k28.evoc.helper.SootHelper.createFlowDiagram;

public class MutationFactory {
    public Class<?> mutate(Class<?> clazz, MType... types) {
    }

    private Class<?> mutateSingle(String className, String methodName, MType type)
            throws ClassNotFoundException {
        SPath sPath = createFlowDiagram(className, methodName);
    }

    private void handleConditionalsBoundary() {
    }
}
