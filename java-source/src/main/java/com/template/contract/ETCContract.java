package com.template.contract;

import net.corda.core.contracts.Contract;
import net.corda.core.contracts.TransactionForContract;
import net.corda.core.crypto.SecureHash;

public class ETCContract implements Contract {

    @Override
    public SecureHash getLegalContractReference() {
        return SecureHash.Companion.sha256("ETC Contract");
    }

    @Override
    public void verify(TransactionForContract tx) {
        throw new UnsupportedOperationException();
    }

}
