package ${package};

import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ${className}Test {
    @Test
    public void test_${methodName}() {
        List<Object> vars = new ArrayList<>();
        ${className} instance = new ${className}();
        <#if returnType != "void">
        ${returnType} result = </#if>instance.${methodName}(<#list parameters as param>${param}<#if param_has_next>, </#if></#list>);

        // TODO: Add assertions here
        // assertEquals(expected, result);
    }
}