package ${cm.packageName};

<#list cm.imports as import>
import ${import}.*;
</#list>

import org.junit.jupiter.api.*;
import com.github.a1k28.supermock.Assertions;

import static org.junit.jupiter.api.Assertions.*;
import static com.github.a1k28.supermock.MockAPI.*;
import static com.github.a1k28.supermock.Parser.deserialize;

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
        <#if mock.paramCount != 0 >
        Object[] params${mock?index} = new Object[${mock.paramCount}];
        // define parameters
        <#list mock.parameters as param>
        <#if param??>
        <#if mock.shouldDeserializeArgs[param?index]>
        params${mock?index}[${param?index}] = deserialize(${param}, ${mock.parameterTypes[param?index]}.class);
        <#else>
        params${mock?index}[${param?index}] = ${param};
        </#if>
        <#else>
        <#if mock.mockType[param?index]??>
        params${mock?index}[${param?index}] = ${mock.mockType[param?index]};
        <#else>
        params${mock?index}[${param?index}] = null;
        </#if>
        </#if>
        </#list>
        <#if mock.exceptionType??>
        when(${mock.type}.class, "${mock.methodName}", params${mock?index}).thenThrow(${mock.exceptionType}.class);
        <#else>
        <#if mock.isStub>
        when(${mock.type}.class, "${mock.methodName}", params${mock?index}).thenReturnStub();
        <#else>
        <#if mock.retVal??>
        <#if mock.shouldDeserializeRetVal>
        ${mock.retType} retVal${mock?index} = deserialize(${mock.retVal}, ${mock.retType}.class);
        <#else>
        ${mock.retType} retVal${mock?index} = ${mock.retVal};
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
        when(${mock.type}.class, "${mock.methodName}").thenReturnStub();
        <#else>
        <#if mock.retVal??>
        <#if mock.shouldDeserializeRetVal>
        ${mock.retType} retVal${mock?index} = deserialize(${mock.retVal}, ${mock.retType}.class);
        <#else>
        ${mock.retType} retVal${mock?index} = ${mock.retVal};
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

        <#if mm.paramCount != 0 >
        // define parameters
        Object[] params = new Object[${mm.paramCount}];
        <#list mm.parameters as param>
            <#--        <#if mm.parameterTypes[param?index] == 'Map' || mm.parameterTypes[param?index] == 'HashMap'>-->
            <#--        Map map${param?index} = new HashMap();-->
            <#--        <#list param.entries as entry>-->
            <#--        map${param?index}.put(deserialize(${entry.key}), deserialize(${entry.value}));-->
            <#--        </#list>-->
            <#--        <#else>-->
        <#if mm.shouldDeserializeArgs[param?index]>
        params[${param?index}] = deserialize(${param}, ${mm.parameterTypes[param?index]}.class);
        <#else>
        params[${param?index}] = ${param};
        </#if>
            <#--        </#if>-->
        </#list>

        </#if>
        <#if mm.exceptionType??>
        assertThrows(${mm.exceptionType}.class, () -> {
            instance.${mm.methodName}(
                <#list 0..<mm.paramCount as i>(${mm.parameterTypes[i]}) params[${i}]<#if i<mm.paramCount-1>, </#if></#list>);
        });
        <#else>
        // call method
        <#if mm.returnType != "void">
        ${mm.returnType} actual = instance.${mm.methodName}(
                <#list 0..<mm.paramCount as i>(${mm.parameterTypes[i]}) params[${i}]<#if i<mm.paramCount-1>, </#if></#list>);
        <#else>
        instance.${mm.methodName}(
                <#list 0..<mm.paramCount as i>(${mm.parameterTypes[i]}) params[${i}]<#if i<mm.paramCount-1>, </#if></#list>);
        </#if>

        <#if mm.returnType != "void">
        // assert
        <#if mm.shouldDeserializeRetVal>
        ${mm.returnType} expected = deserialize(${mm.returnValue}, ${mm.returnType}.class);
        <#else>
        ${mm.returnType} expected = ${mm.returnValue};
        </#if>
        Assertions.assertEquals(expected, actual);
        </#if>
        </#if>
    }

    </#list>
}