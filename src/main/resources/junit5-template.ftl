package ${cm.packageName};

<#list cm.imports as import>
import ${import};
</#list>

<#list cm.staticImports as import>
import static ${import};
</#list>

public class ${cm.className}Test {
    <#if cm.mocksExist == true>
    @BeforeAll
    public static void beforeAll() {
        attachAgentToThisJVM();
    }

    @AfterEach
    public void afterEach() {
        resetMockState();
    }
    </#if>

    <#list cm.methodCallModels as mm>
    @Test
    public void test_${mm.testName}() throws Throwable {
        <#if mm.mockCount != 0 >
        // define mocks
        <#list mm.methodMocks as mock>
        <#if mock.parameters.count != 0>
        <#if mock.isStub == false>
        Object[] params${mock?index} = new Object[${mock.parameters.count}];
        <#list mock.parameters.parameters as param>
        <#if param.value??>
        <#if param.shouldDeserialize>
        params${mock?index}[${param?index}] = deserialize(${param.value}, ${param.type}.class);
        <#else>
        params${mock?index}[${param?index}] = ${param.value}${param.extension};
        </#if>
        <#else>
        <#if param.mockType??>
        params${mock?index}[${param?index}] = ${param.mockType};
        <#else>
        params${mock?index}[${param?index}] = null;
        </#if>
        </#if>
        </#list>
        </#if>
        <#if mock.exceptionType??>
        when(${mock.type}.class, "${mock.methodName}", params${mock?index}).thenThrow(${mock.exceptionType}.class);
        <#else>
        <#if mock.isStub>
        when(${mock.type}.class, "${mock.methodName}"<#list 0..<mock.parameters.count as i>, any()</#list>)
                .thenReturnStub(${mock.retVal.type}.class);
        <#else>
        <#if mock.retVal.value??>
        <#if mock.retVal.shouldDeserialize>
        ${mock.retVal.type} retVal${mock?index} = deserialize(${mock.retVal.value}, ${mock.retVal.type}.class);
        <#else>
        ${mock.retVal.type} retVal${mock?index} = ${mock.retVal.value}${mock.retVal.extension};
        </#if>
        when(${mock.type}.class, "${mock.methodName}", params${mock?index}).thenReturn(retVal${mock?index});
        <#else>
        when(${mock.type}.class, "${mock.methodName}", params${mock?index}).thenReturnVoid();
        </#if>
        </#if>
        </#if>
        <#else>
        <#if mock.exceptionType??>
        when(${mock.type}.class, "${mock.methodName}").thenThrow(${mock.exceptionType}.class);
        <#else>
        <#if mock.isStub>
        when(${mock.type}.class, "${mock.methodName}").thenReturnStub(${mock.retVal.type}.class);
        <#else>
        <#if mock.retVal.value??>
        <#if mock.retVal.shouldDeserialize>
        ${mock.retVal.type} retVal${mock?index} = deserialize(${mock.retVal.value}, ${mock.retVal.type}.class);
        <#else>
        ${mock.retVal.type} retVal${mock?index} = ${mock.retVal.value}${mock.retVal.extension};
        </#if>
        when(${mock.type}.class, "${mock.methodName}").thenReturn(retVal${mock?index});
        <#else>
        when(${mock.type}.class, "${mock.methodName}").thenReturnVoid();
        </#if>
        </#if>
        </#if>
        </#if>

        </#list>
        </#if>
        // assuming default constructor
        ${cm.className} instance = new ${cm.className}();

        <#if mm.fields.count != 0>
        // set fields
        <#list mm.fields.fields as field>
        <#if field.methodExists>
        <#if field.shouldDeserialize>
        <#if field.isStatic>
        ${cm.className}.set${field.nameCapitalized}(deserialize(${field.value}, ${field.type}.class));
        <#else>
        instance.set${field.nameCapitalized}(deserialize(${field.value}, ${field.type}.class));
        </#if>
        <#else>
        <#if field.isStatic>
        ${cm.className}.set${field.nameCapitalized}(${field.value}${field.extension});
        <#else>
        instance.set${field.nameCapitalized}(${field.value}${field.extension});
        </#if>
        </#if>
        <#else>
        <#if field.shouldDeserialize>
        setField(instance, "${field.name}", deserialize(${field.value}, ${field.type}.class));
        <#else>
        setField(instance, "${field.name}", ${field.value}${field.extension});
        </#if>
        </#if>
        </#list>
        </#if>

        <#if mm.parameters.count != 0 >
        // define parameters
        Object[] params = new Object[${mm.parameters.count}];
        <#list mm.parameters.parameters as param>
            <#--        <#if mm.parameterTypes[param?index] == 'Map' || mm.parameterTypes[param?index] == 'HashMap'>-->
            <#--        Map map${param?index} = new HashMap();-->
            <#--        <#list param.entries as entry>-->
            <#--        map${param?index}.put(deserialize(${entry.key}), deserialize(${entry.value}));-->
            <#--        </#list>-->
            <#--        <#else>-->
        <#if param.shouldDeserialize>
        params[${param?index}] = deserialize(${param.value}, ${param.type}.class);
        <#else>
        params[${param?index}] = ${param.value}${param.extension};
        </#if>
        </#list>

        </#if>
        // call method
        <#if mm.exceptionType??>
        assertThrows(${mm.exceptionType}.class, () -> {
            instance.${mm.methodName}(
                <#list 0..<mm.parameters.count as i>(${mm.parameters.parameters[i].type}) params[${i}]<#if i<mm.parameters.count-1>, </#if></#list>);
        });
        <#else>
        <#if mm.retVal.type != "void">
        ${mm.retVal.type} actual = instance.${mm.methodName}(
                <#list 0..<mm.parameters.count as i>(${mm.parameters.parameters[i].type}) params[${i}]<#if i<mm.parameters.count-1>, </#if></#list>);
        <#else>
        instance.${mm.methodName}(
                <#list 0..<mm.parameters.count as i>(${mm.parameters.parameters[i].type}) params[${i}]<#if i<mm.parameters.count-1>, </#if></#list>);
        </#if>

        <#if mm.retVal.type != "void">
        // assert
        <#if mm.retVal.shouldDeserialize>
        ${mm.retVal.type} expected = deserialize(${mm.retVal.value}, ${mm.retVal.type}.class);
        <#else>
        ${mm.retVal.type} expected = ${mm.retVal.value}${mm.retVal.extension};
        </#if>
        Assertions.assertEquals(expected, actual);
        </#if>
        </#if>
    }

    </#list>
}