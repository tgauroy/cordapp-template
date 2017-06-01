package com.etc.flow

import co.paralleluniverse.fibers.Suspendable
import com.etc.contract.SmartLC
import net.corda.core.crypto.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker
import net.corda.flows.AbstractCashFlow
import net.corda.flows.FinalityFlow
import java.security.PublicKey


@CordaSerializable
class CreateSmartLcFlow(val owner: PublicKey,
                        val beneficiary: Party,
                        val issuingBank: Party,
                        val advisingBank: Party,
                        val applicant: Party,
                        val notary: Party,
                        progressTracker: ProgressTracker) : AbstractCashFlow(progressTracker) {
    constructor(owner: PublicKey,
                beneficiary: Party,
                issuingBank: Party,
                advisingBank: Party,
                applicant: Party,
                notary: Party) :
            this(owner, beneficiary, issuingBank, advisingBank, applicant, notary, tracker())


    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = GENERATING_TX
        val builder = SmartLC().generateCreate(owner, beneficiary, issuingBank, advisingBank, applicant, notary)
        progressTracker.currentStep = SIGNING_TX
        val myKey = serviceHub.legalIdentityKey
        builder.signWith(myKey)
        val tx = builder.toSignedTransaction()
        progressTracker.currentStep = FINALISING_TX
        subFlow(FinalityFlow(tx))
        return tx
    }
}

