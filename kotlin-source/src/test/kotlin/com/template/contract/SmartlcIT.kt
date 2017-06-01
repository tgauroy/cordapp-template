package com.template.contract

import com.etc.flow.CreateSmartLcFlow
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import net.corda.contracts.asset.Cash
import net.corda.core.crypto.Party
import net.corda.core.flows.FlowLogic
import net.corda.core.getOrThrow
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.FlowHandle
import net.corda.core.node.services.ServiceInfo
import net.corda.core.node.services.Vault
import net.corda.core.serialization.OpaqueBytes
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.*
import net.corda.flows.CashPaymentFlow
import net.corda.node.driver.driver
import net.corda.node.services.startFlowPermission
import net.corda.nodeapi.User
import net.corda.node.services.transactions.ValidatingNotaryService
import net.corda.testing.expect
import net.corda.testing.expectEvents
import net.corda.testing.parallel
import org.junit.Test
import java.security.PublicKey
import java.util.*


class IntegrationTestSmartLC {

    inline fun <T : Any, A, B, C, D, E, F, reified R : FlowLogic<T>> CordaRPCOps.startFlow4(
            @Suppress("UNUSED_PARAMETER")
            flowConstructor: (A, B, C, D, E, F) -> R,
            arg0: A,
            arg1: B,
            arg2: C,
            arg3: D,
            arg4: E,
            arg5: F
    ): FlowHandle<T> = startFlowDynamic(R::class.java, arg0, arg1, arg2, arg3, arg4, arg5)

    fun startFlowCreateLC(proxy: CordaRPCOps, owner: PublicKey, benef: Party, issuingBk: Party, adviceBk: Party, appli: Party, notary: Party): FlowHandle<SignedTransaction> =
            proxy.startFlow4(::CreateSmartLcFlow, owner, benef, issuingBk, adviceBk, appli, notary)

    @Test
    fun `test integration test with smart lc `() {
        // START 1
        driver (isDebug=true) {
            val beneficiaryUser = User("beneficiaryUser", "testPassword1", permissions = setOf(
                    startFlowPermission<CreateSmartLcFlow>()
            ))

            val IssuingBankUser = User("IssuingBankUser", "testPassword1", permissions = setOf(
                    startFlowPermission<CreateSmartLcFlow>()
            ))

            val AdvisingBankUser = User("AdvisingBankUser", "testPassword1", permissions = setOf(
                    startFlowPermission<CreateSmartLcFlow>()
            ))

            val applicantUser = User("applicantUser", "testPassword1", permissions = setOf(
                    startFlowPermission<CreateSmartLcFlow>()
            ))

            val (benef, issuing, advising, applicant, notary) = Futures.allAsList(
                    startNode(ALICE.name, rpcUsers = listOf(beneficiaryUser)),
                    startNode(DUMMY_BANK_A.name, rpcUsers = listOf(IssuingBankUser)),
                    startNode(DUMMY_BANK_B.name, rpcUsers = listOf(AdvisingBankUser)),
                    startNode(BOB.name, rpcUsers = listOf(applicantUser)),
                    startNode(DUMMY_NOTARY.name, advertisedServices = setOf(ServiceInfo(ValidatingNotaryService.type)))
            ).getOrThrow()
            // END 1

            // START 2
            val benefClient = benef.rpcClientToNode()
            val benefProxy: CordaRPCOps = benefClient.start("beneficiaryUser", "testPassword1").proxy()

//            val issuingBankClient = issuing.rpcClientToNode()
//            val issuingBankProxy = issuingBankClient.start("IssuingBankUser", "testPassword1").proxy()
//
//            val advisingBankClient = advising.rpcClientToNode()
//            val advisingBankProxy = advisingBankClient.start("AdvisingBankUser", "testPassword1").proxy()
//
//            val applicantClient = applicant.rpcClientToNode()
//            val applicantProxy = applicantClient.start("applicantUser", "testPassword1").proxy()
//            // END 2

            // START 3
//            val IssuingBankVaultUpdates = issuingBankProxy.vaultAndUpdates().second
//            val beneficiaryVaultUpdates = benefProxy.vaultAndUpdates().second
//
//            val advisingBankVaultUpdates = advisingBankProxy.vaultAndUpdates().second
//            val applicantVaultUpdates = applicantProxy.vaultAndUpdates().second
            // END 3


            // START 4
            val issueRef = OpaqueBytes.of(0)
            val futures = Stack<ListenableFuture<*>>()

            startFlowCreateLC(benefProxy,
                    ALICE.owningKey,
                    benef.nodeInfo.legalIdentity,
                    issuing.nodeInfo.legalIdentity,
                    advising.nodeInfo.legalIdentity,
                    applicant.nodeInfo.legalIdentity,
                    notary.nodeInfo.notaryIdentity)

//            IssuingBankVaultUpdates.expectEvents {
//                parallel(
//                        (1..10).map { i ->
//                            expect(
//                                    match = { update: Vault.Update ->
//                                        (update.produced.first().state.data as Cash.State).amount.quantity == i * 100L
//                                    }
//                            ) { update ->
//                                println("Bob vault update of $update")
//                            }
//                        }
//                )
//            }
            // END 4

            // START 5

        }
    }
}