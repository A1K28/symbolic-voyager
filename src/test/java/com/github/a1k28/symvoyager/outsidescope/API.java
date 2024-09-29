package com.github.a1k28.symvoyager.outsidescope;

import com.github.a1k28.symvoyager.core.z3extended.model.StatusDTO;

public interface API {
    String authenticate(int k);
    StatusDTO getCustomerInfoStatus(String customerId);
}
