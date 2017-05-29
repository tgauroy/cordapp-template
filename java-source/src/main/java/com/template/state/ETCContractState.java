package com.template.state;

import com.google.common.collect.ImmutableList;
import com.template.contract.ETCContract;
import kotlin.Pair;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;

import net.corda.core.contracts.OwnableState;
import org.jetbrains.annotations.NotNull;


import java.security.PublicKey;
import java.time.Instant;
import java.util.List;

public class ETCContractState implements OwnableState {

    private Instant etcReferenceID;
    private PublicKey owner;

    public ETCContractState() {
    }

    public ETCContractState(Instant etcReferenceID, PublicKey owner) {
        this.etcReferenceID = etcReferenceID;
        this.owner = owner;
    }

    @NotNull
    @Override
    public PublicKey getOwner() {
        return null;
    }

    @NotNull
    @Override
    public Pair<CommandData, OwnableState> withNewOwner(PublicKey publicKey) {
        return null;
    }

    @NotNull
    @Override
    public Contract getContract() {
        return new ETCContract();
    }

    @NotNull
    public List<PublicKey> getParticipants() {
        return ImmutableList.of(this.owner);
    }
}
