package com.github.a1k28.evoc.core.z3extended.struct;

import com.github.a1k28.evoc.core.z3extended.model.CustomerInfoDTO;
import com.github.a1k28.evoc.core.z3extended.model.StatusDTO;
import com.github.a1k28.junitengine.SymbolicTest;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

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

    @SymbolicTest({0,1,2})
    @DisplayName("test_object_winput_1")
    public int test_object_winput_1(StatusDTO statusDTO, int expected, String message) {
        if (statusDTO.getStatus() == expected)
            return 0;
        if (statusDTO.getMessage().equals(message))
            return 1;
        return 2;
    }

    @SymbolicTest({0,1,2,3,4,5,6})
    @DisplayName("test_object_winput_2")
    public int test_object_winput_2(CustomerInfoDTO customerInfoDTO,
                                    String customerId,
                                    int expected,
                                    String message) {
        if (customerInfoDTO.getCustomerId().equals(customerId))
            return 0;
        if (customerInfoDTO.getCustomerId().equals("ASDAWD"))
            return 1;
        String cId = customerInfoDTO.getCustomerId();
        if ("ASDASDASDADS".equals(cId))
            return 2;
        if (customerInfoDTO.getDebt() == 20)
            return 3;
        if (customerInfoDTO.getStatus().getStatus() == expected)
            return 4;
        if (customerInfoDTO.getStatus().getMessage().equals(message))
            return 5;
        return 6;
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

    @SymbolicTest({2,3})
    @DisplayName("test_object_inside_list_1")
    public int test_object_inside_list_1(String customerId) {
        CustomerInfoDTO dto1 = new CustomerInfoDTO();
        dto1.setCustomerId(customerId);

        CustomerInfoDTO dto2 = new CustomerInfoDTO();
        dto2.setCustomerId(customerId+"ASD");

        List<CustomerInfoDTO> list = new ArrayList();
        list.add(dto1);
        list.add(dto2);

        if (!list.get(0).getCustomerId().equals(customerId))
            return 0;
        if (!list.get(1).getCustomerId().equals(customerId+"ASD"))
            return 1;
        if (list.get(1).getCustomerId().equals("afjawifjASD"))
            return 2;
        return 3;
    }
}