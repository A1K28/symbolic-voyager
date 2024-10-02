package com.github.a1k28.symvoyager.core.assembler;

import com.github.a1k28.supermock.MockType;
import com.github.a1k28.supermock.Parser;
import com.github.a1k28.symvoyager.core.assembler.model.*;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.MethodMockResult;
import com.github.a1k28.symvoyager.core.symbolicexecutor.model.ParsedResult;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SVar;
import com.github.a1k28.symvoyager.core.symbolicexecutor.struct.SVarEvaluated;
import com.github.a1k28.symvoyager.helper.Logger;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.*;

import static com.github.a1k28.symvoyager.core.sootup.SootInterpreter.translateField;

public class JUnitTestAssembler {
    private static final Logger log = Logger.getInstance(JUnitTestAssembler.class);

    public static void assembleTest(Class<?> clazz,
                                    List<TestGeneratorModel> testGeneratorModels)
            throws IOException, TemplateException {
        String className = clazz.getSimpleName();
        String packageName = clazz.getPackageName();
        String testClassName = className + "Test";
        String targetPath = clazz.getProtectionDomain().getCodeSource().getLocation().getPath().replace("/", File.separator);
        String testPath = targetPath.replace(File.separator+"target"+File.separator+"classes", File.separator+"src"+File.separator+"test"+File.separator+"java"+File.separator);
        String fileName = testPath + packageName.replace(".", File.separator) + File.separator + testClassName + ".java";
        Set<String> imports = new HashSet<>();
        Set<String> staticImports = new HashSet<>();
        List<MethodCallModel> methodCallModels = new ArrayList<>();
        Map<String, Integer> nameCount = new HashMap<>();
        boolean mocksExist = false;

        log.trace("targetPath: " + targetPath);

        for (TestGeneratorModel testGeneratorModel : testGeneratorModels) {
            Method method = testGeneratorModel.getMethod();
            ParsedResult res = testGeneratorModel.getParsedResult();
            Class retType = res.getReturnType();
            if (res.getParsedParameters().length != method.getParameterCount()) {
                log.warn("Invalid number of arguments passed for java test assembly");
                return;
            }

            List<Parameter> parameters = new ArrayList<>();
            for (int i = 0; i < res.getParsedParameters().length; i++) {
                Parameter parameter = new Parameter();
                parameter.setValue(parse(res.getParsedParameters()[i], method.getParameterTypes()[i]));
                parameter.setShouldDeserialize(shouldSerialize(method.getParameterTypes()[i]));
                parameter.setExtension(getExtension(method.getParameterTypes()[i]));
                parameter.setType(method.getParameterTypes()[i].getSimpleName());
                parameters.add(parameter);
            }
            Parameters parametersModel = new Parameters(
                    parameters.size(), parameters);

            List<Field> fields = new ArrayList<>();
            for (SVarEvaluated fieldVar : res.getParsedFields()) {
                java.lang.reflect.Field jField = getField(fieldVar.getSvar(), clazz);
                Object val = parse(fieldVar.getEvaluated(), jField.getType());
                Object defaultVal = getDefaultFieldValue(jField.getType());
                if (val == null || val.equals(defaultVal))
                    continue;

                Field field = new Field();
                field.setName(jField.getName());
                field.setNameCapitalized(capitalize(jField.getName()));
                field.setValue(val);
                field.setShouldDeserialize(shouldSerialize(jField.getType()));
                field.setExtension(getExtension(jField.getType()));
                field.setType(jField.getType().getSimpleName());
                field.setIsStatic(java.lang.reflect.Modifier.isStatic(jField.getModifiers()));
                field.setMethodExists(setterExists(jField, field.getNameCapitalized()));
                fields.add(field);

                if (!jField.getType().isPrimitive())
                    imports.add(jField.getType().getPackageName()+".*");
            }
            Fields fieldsModel = new Fields(fields.size(), fields);

            // return val
            Class<? extends Throwable> exceptionType = testGeneratorModel.getParsedResult().getExceptionType();
            Object retValue = exceptionType == null ?
                    parse(res.getParsedReturnValue(), retType) : null;
            String exceptionTypeStr = exceptionType == null ? null : exceptionType.getSimpleName();

            Parameter retVal = new Parameter();
            retVal.setValue(retValue);
            retVal.setType(retType.getSimpleName());
            retVal.setShouldDeserialize(shouldSerialize(retType));
            retVal.setExtension(getExtension(retType));

            // handle imports
            if (exceptionType != null) {
                imports.add(exceptionType.getPackageName()+".*");
            }

            for (Class parameterType : method.getParameterTypes()) {
                if (!parameterType.isPrimitive())
                    imports.add(parameterType.getPackageName()+".*");
            }

            for (MethodMockResult mockResult : res.getMethodMockValues()) {
                imports.add(mockResult.getMethod().getReturnType().getPackageName()+".*");
                imports.add(mockResult.getMethod().getDeclaringClass().getPackageName()+".*");
                if (mockResult.getExceptionType() != null)
                    imports.add(mockResult.getExceptionType().getPackageName()+".*");
                for (Object param : mockResult.getParsedParameters())
                    if (param != null) imports.add(param.getClass().getPackageName()+".*");
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
                    exceptionTypeStr,
                    fieldsModel,
                    retVal,
                    parametersModel,
                    mockModels.size(),
                    mockModels);
            methodCallModels.add(methodCallModel);
        }

        imports.add("org.junit.jupiter.api.*");
        imports.add("com.github.a1k28.supermock.*");

        staticImports.add("org.junit.jupiter.api.Assertions.*");
        staticImports.add("com.github.a1k28.supermock.MockAPI.*");
        staticImports.add("com.github.a1k28.supermock.Parser.deserialize");

        ClassModel classModel = new ClassModel(
                packageName,
                className,
                new ArrayList<>(imports),
                new ArrayList<>(staticImports),
                methodCallModels,
                mocksExist);

        Map<String, Object> data = new HashMap<>();
        data.put("cm", classModel);

        // Configure Freemarker
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(JUnitTestAssembler.class, File.separator);
        Template template = cfg.getTemplate("junit5-template.ftl");

        // Write to file
        File f = new File(fileName);
        f.getParentFile().mkdirs();
        try (Writer writer = new FileWriter(fileName)) {
            template.process(data, writer);
        }

        log.info("Test file generated: " + fileName);
    }

    // TODO: beautify certain types of object creation
    private static Object parse(Object object, Class<?> clazz) {
        if (object instanceof MockType)
            return null;
        if (clazz == String.class)
            return "\""+object+"\"";
        if (clazz == boolean.class || clazz == Boolean.class)
            return String.valueOf(object);
        if (!shouldSerialize(clazz))
            return object;
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
            Class retType = mockResult.getReturnType();
            Class exceptionType = mockResult.getExceptionType();
            String retTypeStr;
            if ("void".equals(mockResult.getMethod().getReturnType().getName()))
                retTypeStr = null;
            else
                retTypeStr = retType.getSimpleName();

            String exceptionName = exceptionType == null ? null : exceptionType.getSimpleName();

            Parameter retVal = new Parameter();
            retVal.setValue(parse(mockResult.getParsedReturnValue(), retType));
            retVal.setType(retTypeStr);
            retVal.setShouldDeserialize(shouldSerialize(retType));
            retVal.setExtension(getExtension(retType));

            List<MockParameter> mockParameters = new ArrayList<>();
            for (int i = 0; i < args.length; i++) {
                MockParameter mockParameter = new MockParameter();
                mockParameter.setValue(parse(args[i], method.getParameterTypes()[i]));
                mockParameter.setShouldDeserialize(shouldSerialize(method.getParameterTypes()[i]));
                mockParameter.setExtension(getExtension(method.getParameterTypes()[i]));
                mockParameter.setType(method.getParameterTypes()[i].getSimpleName());
                mockParameter.setMockType(args[i] instanceof MockType ? "any()" : null);
                mockParameters.add(mockParameter);
            }
            MockParameters mockParametersModel = new MockParameters(
                    mockParameters.size(), mockParameters);

            boolean isStub = mockResult.getParsedReturnValue() instanceof MockType i && i == MockType.STUB;

            MethodMockModel model = new MethodMockModel(
                    type.getSimpleName(),
                    method.getName(),
                    mockParametersModel,
                    retVal,
                    isStub,
                    exceptionName);
            result.add(model);
        }
        return result;
    }

    private static Object getDefaultFieldValue(Class type) {
        if (type == boolean.class || type == Boolean.class)
            return false;
        if (type == byte.class || type == Byte.class)
            return 0;
        if (type == short.class || type == Short.class)
            return 0;
        if (type == char.class || type == Character.class)
            return '\u0000';
        if (type == int.class || type == Integer.class)
            return 0;
        if (type == long.class || type == Long.class)
            return 0;
        if (type == float.class || type == Float.class)
            return 0;
        if (type == double.class || type == Double.class)
            return 0;
        return null;
    }

    private static boolean shouldSerialize(Class clazz) {
        return !(clazz == String.class || clazz == Class.class ||
                clazz == Boolean.class || clazz == boolean.class ||
                clazz == Byte.class || clazz == byte.class ||
                clazz == Short.class || clazz == short.class ||
                clazz == Integer.class || clazz == int.class ||
                clazz == Long.class || clazz == long.class ||
                clazz == Float.class || clazz == float.class ||
                clazz == Double.class || clazz == double.class ||
                clazz == Character.class || clazz == char.class);
    }

    private static String getExtension(Class clazz) {
        if (clazz == Long.class || clazz == long.class) return "l";
        if (clazz == Float.class || clazz == float.class) return "f";
        if (clazz == Double.class || clazz == double.class) return "d";
        return "";
    }

    private static String capitalize(String name) {
        return name.substring(0, 1).toUpperCase() + name.substring(1);
    }

    private static boolean setterExists(java.lang.reflect.Field field, String name) {
        String finalName = "set"+name;
        return Arrays.stream(field.getDeclaringClass().getDeclaredMethods())
                .anyMatch(method -> method.getName().equals(finalName));
    }


    private static java.lang.reflect.Field getField(SVar var, Class clazz) {
        for (java.lang.reflect.Field field : clazz.getDeclaredFields()) {
            if (equalsField(var, field)) return field;
        }
        throw new RuntimeException("Could not match fields: " + var
                + " with: " + Arrays.toString(clazz.getDeclaredFields()));
    }

    private static boolean equalsField(SVar sVar, java.lang.reflect.Field field) {
        return translateField(field).equals(sVar.getName());
    }
}
