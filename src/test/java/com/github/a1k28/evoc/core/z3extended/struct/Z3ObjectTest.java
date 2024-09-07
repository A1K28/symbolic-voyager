package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.CustomerInfoDTO;
import com.github.a1k28.evoc.core.z3extended.model.StatusDTO;
import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

public class Z3ObjectTest {
    @SymbolicTest({0,1,2,3,4})
    @DisplayName("test_copy_of_1")
    public int test_copy_of_1(String customerId, int debt, int status, String message) {
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
}