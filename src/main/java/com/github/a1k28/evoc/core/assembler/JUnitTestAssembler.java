package com.github.a1k28.evoc.core.assembler;

import com.github.a1k28.evoc.core.assembler.model.ClassModel;
import com.github.a1k28.evoc.core.assembler.model.MapModel;
import com.github.a1k28.evoc.core.assembler.model.MethodCallModel;
import com.github.a1k28.evoc.core.assembler.model.MethodMockModel;
import com.github.a1k28.evoc.core.symbolicexecutor.model.MethodMockResult;
import com.github.a1k28.evoc.helper.Logger;
import com.github.a1k28.supermock.Parser;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.*;

public class JUnitTestAssembler {
    private static final Logger log = Logger.getInstance(JUnitTestAssembler.class);

    public static void assembleTest(Class<?> clazz,
                                    String methodName,
                                    Object returnValue,
                                    List<Object> params,
                                    List<MethodMockResult> methodMocks)
            throws IOException, TemplateException {
        if (params.isEmpty()) {
            log.warn("No arguments passed for java test assembly");
            return;
        }

        Method method = findMethod(clazz, methodName, params);
        String className = clazz.getSimpleName();
        String testClassName = className + "Test";
        String fileName = clazz.getProtectionDomain().getCodeSource().getLocation().getPath().replace("/target/classes", "/src/test/java/") + clazz.getPackageName().replace(".", File.separator) + File.separator + testClassName + ".java";
        List<String> parameterTypes = Arrays.stream(method.getParameterTypes())
                .map(Class::getSimpleName).toList();

        List<Object> parameters = new ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            parameters.add(parse(params.get(i), method.getParameterTypes()[i]));
        }

        Set<String> imports = new HashSet<>();
        for (Class parameterType : method.getParameterTypes()) {
            if (!parameterType.isPrimitive())
                imports.add(parameterType.getPackageName());
        }

//        for (MethodMockResult mockResult : methodMocks) {
//            imports.add(mockResult.getExceptionType().getPackageName());
//            for (Object param : mockResult.getParsedParameters())
//                imports.add(param.getClass().getPackageName());
//        }

        // Prepare the data for the template
        MethodCallModel methodCallModel = new MethodCallModel(
                methodName,
                method.getReturnType().getSimpleName(),
                returnValue,
                params.size(),
                parameters,
                parameterTypes,
                mapMocks(methodMocks));

        ClassModel classModel = new ClassModel(
                clazz.getPackageName(),
                className,
                new ArrayList<>(imports),
                List.of(methodCallModel));

        Map<String, Object> data = new HashMap<>();
        data.put("cm", classModel);

        // Configure Freemarker
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(JUnitTestAssembler.class, "/");
        Template template = cfg.getTemplate("junit5-template.ftl");

//        // Write to file
        File f = new File(fileName);
        f.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(fileName)) {
            template.process(data, writer);
        }

        log.info("Test file generated: " + fileName);
    }

    private static Object parse(Object object, Class<?> clazz) {
//        if (Map.class.isAssignableFrom(clazz)) {
//            List<MapModel.Entry> entries = new ArrayList<>();
//            for (Map.Entry<?,?> entry : ((Map<?,?>)object).entrySet()) {
//                entries.add(new MapModel.Entry(
//                        Parser.serialize(entry.getKey()),
//                        Parser.serialize(entry.getValue())));
//            }
//            MapModel mapModel = new MapModel(entries);
//            return mapModel;
//        }
        return Parser.serialize(object);
    }

    private static List<MethodMockModel> mapMocks(List<MethodMockResult> methodMocks) {
        if (methodMocks == null || methodMocks.isEmpty()) Collections.emptyList();
        List<MethodMockModel> result = new ArrayList<>(methodMocks.size());
        for (MethodMockResult mockResult : methodMocks) {
//            Class type = mockResult.getMethod().getDeclaringClass();
//            String methodName = mockResult.getMethod().getName();
//            Object[] args = mockResult.getParsedParameters().toArray();
//            Object retVal = mockResult.getParsedReturnValue();
//            Class exceptionType = mockResult.getExceptionType();
//            MethodMockModel model = new MethodMockModel(
//                    type.getCanonicalName(), methodName, args, retVal, exceptionType);
//            result.add(model);
        }
        return result;
    }

    private static Method findMethod(Class clazz, String methodName, List<Object> args) {
        outer: for (Method method : clazz.getDeclaredMethods()) {
            if (!method.getName().equals(methodName)) continue;
            if ((args == null || args.isEmpty()) ^ method.getParameterCount() == 0) continue;
            if (args == null) return method;
            if (args.size() != method.getParameterCount()) continue;
            for (int i = 0; i < args.size(); i++) {
                if (!equalsClasses(method.getParameterTypes()[i], args.get(i).getClass())) continue outer;
            }
            return method;
        }
        throw new RuntimeException("Declared method: " + methodName + " not found for class: " + clazz);
    }

    private static boolean equalsClasses(Class<?> c1, Class<?> c2) {
        if (c1.equals(c2)) return true;
        if (c1.isAssignableFrom(c2)) return true;

        if (c1 == byte.class && c2 == Byte.class) return true;
        if (c2 == byte.class && c1 == Byte.class) return true;

        if (c1 == short.class && c2 == Short.class) return true;
        if (c2 == short.class && c1 == Short.class) return true;

        if (c1 == int.class && c2 == Integer.class) return true;
        if (c2 == int.class && c1 == Integer.class) return true;

        if (c1 == long.class && c2 == Long.class) return true;
        if (c2 == long.class && c1 == Long.class) return true;

        if (c1 == char.class && c2 == Character.class) return true;
        if (c2 == char.class && c1 == Character.class) return true;

        if (c1 == boolean.class && c2 == Boolean.class) return true;
        if (c2 == boolean.class && c1 == Boolean.class) return true;

        if (c1 == float.class && c2 == Float.class) return true;
        if (c2 == float.class && c1 == Float.class) return true;

        if (c1 == double.class && c2 == Double.class) return true;
        if (c2 == double.class && c1 == Double.class) return true;

        return false;
    }

    public static void main(String[] args) throws Exception {
        List<Object> params = new ArrayList<>(2);
        Map map = new HashMap();
        map.put("ASD", "Ads");
        map.put("ASawdawdD", "A1242d214s");
        map.put("ASadwadawdD", "124Aawdds");
        map.put("AawdawdSD", "Aawdds");
        map.put("Asad1231231SD", "awdawdAds");
        map.put("A214SD", "Adawdawds");
        map.put("ASDadawdawd", "Aawdawds");
        params.add(0, "ASD");
        params.add(1, map);
        Class<?> clazz = Class.forName("com.github.a1k28.Stack");
        JUnitTestAssembler assembler = new JUnitTestAssembler();
        MethodMockResult methodMockResult = new MethodMockResult(
                null, null, null, null);
        assembler.assembleTest(clazz, "test_method_call_with_map",
                "null", params, List.of(methodMockResult));
    }
}
