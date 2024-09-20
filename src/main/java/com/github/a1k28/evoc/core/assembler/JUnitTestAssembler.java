package com.github.a1k28.evoc.core.assembler;

import com.github.a1k28.evoc.core.assembler.model.*;
import com.github.a1k28.evoc.core.symbolicexecutor.model.MethodMockResult;
import com.github.a1k28.evoc.core.symbolicexecutor.model.ParsedResult;
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
                                    List<TestGeneratorModel> testGeneratorModels)
            throws IOException, TemplateException {
        String className = clazz.getSimpleName();
        String testClassName = className + "Test";
        String targetPath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath();
        String testPath = targetPath.replace(File.separator+"target"+File.separator+"classes", File.separator+"src"+File.separator+"test"+File.separator+"java"+File.separator);
        String fileName = testPath + clazz.getPackageName().replace(".", File.separator) + File.separator + testClassName + ".java";
        Set<String> imports = new HashSet<>();
        List<MethodCallModel> methodCallModels = new ArrayList<>();
        Map<String, Integer> nameCount = new HashMap<>();
        boolean mocksExist = false;

        for (TestGeneratorModel testGeneratorModel : testGeneratorModels) {
            Method method = testGeneratorModel.getMethod();
            ParsedResult res = testGeneratorModel.getParsedResult();
            if (res.getParsedParameters().length != method.getParameterCount()) {
                log.warn("Invalid number of arguments passed for java test assembly");
                return;
            }

            List<String> parameterTypes = Arrays.stream(method.getParameterTypes())
                    .map(Class::getSimpleName).toList();

            List<Object> parameters = new ArrayList<>();
            for (int i = 0; i < res.getParsedParameters().length; i++) {
                parameters.add(parse(res.getParsedParameters()[i], method.getParameterTypes()[i]));
            }

            // handle imports
            for (Class parameterType : method.getParameterTypes()) {
                if (!parameterType.isPrimitive())
                    imports.add(parameterType.getPackageName());
            }

            for (MethodMockResult mockResult : res.getMethodMockValues()) {
                imports.add(mockResult.getMethod().getReturnType().getPackageName());
                imports.add(mockResult.getMethod().getDeclaringClass().getPackageName());
                if (mockResult.getExceptionType() != null)
                    imports.add(mockResult.getExceptionType().getPackageName());
                for (Object param : mockResult.getParsedParameters())
                    if (param != null) imports.add(param.getClass().getPackageName());
            }

            if (!nameCount.containsKey(method.getName()))
                nameCount.put(method.getName(), 0);
            nameCount.put(method.getName(), nameCount.get(method.getName())+1);
            String testName = method.getName()+"_"+nameCount.get(method.getName());

            List<MethodMockModel> mockModels = mapMocks(res.getMethodMockValues());
            if (!mockModels.isEmpty())
                mocksExist = true;

            // Prepare the data for the template
            MethodCallModel methodCallModel = new MethodCallModel(
                    testName,
                    method.getName(),
                    method.getReturnType().getSimpleName(),
                    parse(res.getParsedReturnValue(), method.getReturnType()),
                    res.getParsedParameters().length,
                    parameters,
                    parameterTypes,
                    mockModels.size(),
                    mockModels);
            methodCallModels.add(methodCallModel);
        }

        ClassModel classModel = new ClassModel(
                clazz.getPackageName(),
                clazz.getSimpleName(),
                new ArrayList<>(imports),
                methodCallModels,
                mocksExist);

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

    // TODO: beautify certain types of object creation
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
            Class type = mockResult.getMethod().getDeclaringClass();
            Method method = mockResult.getMethod();
            Object[] args = mockResult.getParsedParameters().toArray();
            Object retVal = mockResult.getParsedReturnValue();
            Class exceptionType = mockResult.getExceptionType();

            String retValSerialized = Parser.serialize(retVal);
            String exceptionName = exceptionType == null ? null : exceptionType.getSimpleName();
            String retType = method.getReturnType().getSimpleName();
            if ("void".equals(retType)) retType = null;

            List<String> parameterTypes = Arrays.stream(method.getParameterTypes())
                    .map(Class::getSimpleName).toList();
            List<Object> parameters = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                parameters.add(parse(args[i], method.getParameterTypes()[i]));
            }

            MethodMockModel model = new MethodMockModel(
                    type.getSimpleName(),
                    method.getName(),
                    parameters.size(),
                    parameters,
                    parameterTypes,
                    retValSerialized,
                    retType,
                    exceptionName);
            result.add(model);
        }
        return result;
    }
}
