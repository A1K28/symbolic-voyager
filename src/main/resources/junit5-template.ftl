package ${cm.packageName};

<#list cm.imports as import>
import ${import}.*;
</#list>

import org.junit.jupiter.api.*;
import com.github.a1k28.supermock.MockAPI;

import static org.junit.jupiter.api.Assertions.*;
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
    public void test_${mm.methodName}() throws Throwable {
        // define mocks
<#--        <#list mocks as mock>-->
<#--            ${mock.o}-->
<#--        </#list>-->

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

        // assuming default constructor
        ${cm.className} instance = new ${cm.className}();

        // call method
        <#if mm.returnType != "void">
        ${mm.returnType} result = </#if>instance.${mm.methodName}(
            <#list 0..<mm.paramCount as i>(${mm.parameterTypes[i]}) params[${i}]<#if i<mm.paramCount-1>, </#if></#list>);

        // assert
        com.github.a1k28.supermock.Assertions.assertEquals(${mm.returnValue}, result);
    }

</#list>
}