package com.github.a1k28.evoc.core.assembler;

import com.github.a1k28.evoc.helper.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JUnitTestAssembler {
    private static final Logger log = Logger.getInstance(JUnitTestAssembler.class);

    public void assembleTests(Class<?> clazz, String methodName, List<List<Object>> params) {
    }

    private void assembleTest(Class<?> clazz, String methodName, List<Object> params)
            throws IOException, TemplateException {
        if (params.isEmpty()) {
            log.warn("No arguments passed for java test assembly");
            return;
        }

        Method method = findMethod(clazz, methodName, params);
        String className = clazz.getSimpleName();
        String testClassName = className + "Test";
        String fileName = "src/test/java/"+clazz.getPackageName().replace(".", File.separator) + File.separator + testClassName + ".java";

        // Prepare the data for the template
        Map<String, Object> data = new HashMap<>();
        data.put("package", clazz.getPackageName());
        data.put("className", className);
        data.put("methodName", methodName);
        data.put("returnType", method.getReturnType().getSimpleName());

        //            Map<String, String> paramMap = new HashMap<>();
        //            paramMap.put("dummyValue", "AWDAWD");
        //            parameters.add();
//        List<Object> parameters = new ArrayList<>(params);
        data.put("parameters", mapVariables(method.getParameters(), params));

        // Configure Freemarker
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(JUnitTestAssembler.class, "/");
        Template template = cfg.getTemplate("junit5-template.ftl");

//        // Write to file
        try (Writer writer = new FileWriter(fileName)) {
            template.process(data, writer);
        }

        log.info("Test file generated: " + fileName);
    }

    private List<Object> mapVariables(Parameter[] mParams, List<Object> params) {
        List<Object> result = new ArrayList<>(params.size());
        for (int i = 0; i < mParams.length; i++) {
            if (mParams[i].getType() == String.class) result.add("\"" + params.get(i) + "\"");
            else if (mParams[i].getType().isPrimitive()) result.add(params.get(i));
            else throw new RuntimeException("Could not map variable type: " + mParams[i].getType());
        }
        return result;
    }

    private Method findMethod(Class<?> clazz, String name, List<Object> params) {
        Method candidate = null;
        outer: for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(name)) continue;
            Class<?>[] parameterTypes = method.getParameterTypes();
            for (int i = 0; i < parameterTypes.length; i++) {
                if (params.get(i) != null && params.get(i).getClass() != parameterTypes[i])
                    continue outer;
            }
            if (candidate != null)
                throw new RuntimeException("Multiple methods found:" + name);
            candidate = method;
        }
        if (candidate != null) return candidate;
        throw new RuntimeException("Method not found: " + name);
    }

    public static void main(String[] args) throws Exception {
        List<Object> params = new ArrayList<>(2);
        params.add(0, "ASD");
        params.add(1, "");
        Class<?> clazz = Class.forName("com.github.a1k28.Stack");
        JUnitTestAssembler assembler = new JUnitTestAssembler();
        assembler.assembleTest(clazz, "test_method_call", params);
    }
}
