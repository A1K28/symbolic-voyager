package ${cm.packageName};

<#list cm.imports as import>
import ${import}.*;
</#list>

import org.junit.jupiter.api.*;
import com.github.a1k28.supermock.MockAPI;
import com.github.a1k28.supermock.Assertions;

import static org.junit.jupiter.api.Assertions.*;
import static com.github.a1k28.supermock.MockAPI.when;
import static com.github.a1k28.supermock.Parser.deserialize;

public class ${cm.className}Test {
    @BeforeAll
    public static void beforeAll() {
        MockAPI.attachAgentToThisJVM();
    }

    @AfterEach
    public void afterEach() {
        MockAPI.resetMockState();
    }

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
        params${mock?index}[${param?index}] = deserialize(${param}, ${mock.parameterTypes[param?index]}.class);
        </#list>
        <#if mock.exceptionType??>
        when(${mock.type}.class, "${mock.methodName}", params${mock?index}).thenThrow(${mock.exceptionType}.class);
        <#else>
        <#if mock.retVal??>
        ${mock.retType} retVal${mock?index} = deserialize(${mock.retVal}, ${mock.retType}.class);
        when(${mock.type}.class, "${mock.methodName}", params${mock?index}).thenReturn(retVal${mock?index});
        <#else>
        when(${mock.type}.class, "${mock.methodName}", params${mock?index}).thenReturnVoid();
        </#if>
        </#if>
        <#else>
        <#if mock.exceptionType??>
        when(${mock.type}.class, "${mock.methodName}").thenThrow(${mock.exceptionType}.class);
        <#else>
        <#if mock.retVal??>
        ${mock.retType} retVal${mock?index} = deserialize(${mock.retVal}, ${mock.retType}.class);
        when(${mock.type}.class, "${mock.methodName}").thenReturn(retVal${mock?index});
        <#else>
        when(${mock.type}.class, "${mock.methodName}").thenReturnVoid();
        </#if>
        </#if>
        </#if>

        </#list>
        </#if>
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
        params[${param?index}] = deserialize(${param}, ${mm.parameterTypes[param?index]}.class);
            <#--        </#if>-->
            </#list>

        </#if>
        // assuming default constructor
        ${cm.className} instance = new ${cm.className}();

        // call method
        <#if mm.returnType != "void">
        ${mm.returnType} actual = </#if>instance.${mm.methodName}(
            <#list 0..<mm.paramCount as i>(${mm.parameterTypes[i]}) params[${i}]<#if i<mm.paramCount-1>, </#if></#list>);

        <#if mm.returnType != "void">
        // assert
        ${mm.returnType} expected = deserialize(${mm.returnValue}, ${mm.returnType}.class);
        Assertions.assertEquals(expected, actual);
        </#if>
    }

    </#list>
}