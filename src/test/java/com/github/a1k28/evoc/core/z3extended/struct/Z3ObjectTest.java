package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.CustomerInfoDTO;
import com.github.a1k28.evoc.core.z3extended.model.StatusDTO;
import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

public class Z3ObjectTest {
    @SymbolicTest({2})
    @DisplayName("test_object_1")
    public int test_object_1(String customerId) {
        CustomerInfoDTO customerInfoDTO = new CustomerInfoDTO();
        customerInfoDTO.setCustomerId(customerId);
        if (customerInfoDTO.getCustomerId() != customerId)
            return 0;

        customerInfoDTO.setCustomerId(customerId+1);
        if (customerInfoDTO.getCustomerId() == customerId)
            return 1;

        return 2;
    }

    @SymbolicTest({0,1,2,3,4})
    @DisplayName("test_object_nested_1")
    public int test_object_nested_1(String customerId, int debt, int status, String message) {
        StatusDTO statusDTO = new StatusDTO();
        statusDTO.setStatus(status);
        statusDTO.setMessage(message);

        CustomerInfoDTO customerInfoDTO = new CustomerInfoDTO();
        customerInfoDTO.setCustomerId(customerId);
        customerInfoDTO.setDebt(debt);
        customerInfoDTO.setStatus(statusDTO);

        if ("CUSTOMER_ID".equals(customerInfoDTO.getCustomerId()))
            return 0;
        if (200 == customerInfoDTO.getDebt())
            return 1;
        if (0 == customerInfoDTO.getStatus().getStatus())
            return 2;
        if ("SUCCESS".equals(customerInfoDTO.getStatus().getMessage()))
            return 3;
        return 4;
    }

    @SymbolicTest({0,1,2,3,4})
    @DisplayName("test_object_nested_with_constructor_1")
    public int test_object_nested_with_constructor_1(
            String customerId, int debt, int status, String message) {
        StatusDTO statusDTO = new StatusDTO();
        statusDTO.setStatus(status);
        statusDTO.setMessage(message);

        CustomerInfoDTO customerInfoDTO = new CustomerInfoDTO(customerId);
        customerInfoDTO.setDebt(debt);
        customerInfoDTO.setStatus(statusDTO);

        if ("CUSTOMER_ID".equals(customerInfoDTO.getCustomerId()))
            return 0;
        if (200 == customerInfoDTO.getDebt())
            return 1;
        if (0 == customerInfoDTO.getStatus().getStatus())
            return 2;
        if ("SUCCESS".equals(customerInfoDTO.getStatus().getMessage()))
            return 3;
        return 4;
    }

    @SymbolicTest({2})
    @DisplayName("test_object_uncertain_fields_1")
    public int test_object_uncertain_fields_1(String customerId, int debt) {
        CustomerInfoDTO customerInfoDTO = new CustomerInfoDTO();
        customerInfoDTO.setDebt(debt);

        if (customerId.equals(customerInfoDTO.getCustomerId()))
            return 0;
        if (debt != customerInfoDTO.getDebt())
            return 1;
        return 2;
    }

    @SymbolicTest({2})
    @DisplayName("test_object_uncertain_fields_2")
    public int test_object_uncertain_fields_2(int debt) {
        CustomerInfoDTO customerInfoDTO = new CustomerInfoDTO();
        customerInfoDTO.setDebt(debt);

        if (customerInfoDTO.getCustomerId() != null)
            return 0;
        if (debt != customerInfoDTO.getDebt())
            return 1;
        if (null == customerInfoDTO.getStatus())
            return 2;
        return 3;
    }
}