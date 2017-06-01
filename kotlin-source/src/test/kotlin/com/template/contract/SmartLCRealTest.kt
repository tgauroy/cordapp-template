package com.template.contract

import com.etc.contract.*
import com.etc.contract.status.SmartLCStatus
import junit.framework.Assert.assertEquals
import net.corda.core.utilities.DUMMY_NOTARY
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.Party
import net.corda.core.crypto.SecureHash
import net.corda.core.getOrThrow
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.DUMMY_NOTARY_KEY
import net.corda.core.utilities.DUMMY_PUBKEY_1
import net.corda.node.utilities.transaction
import net.corda.testing.ledger
import net.corda.testing.node.MockNetwork
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import net.corda.flows.ResolveTransactionsFlow
import net.corda.testing.MEGA_CORP_KEY
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.security.KeyPair


open class SmartLCRealTest {

    val beneficiaryKeyPair = generateKeyPair()
    val issuingBankKeyPair = generateKeyPair()
    val advisingBankKeyPair = generateKeyPair()
    val applicantKeyPair = generateKeyPair()
    val otherplayerwedontcareKeyPair = generateKeyPair()


    val beneficiary = Party("beneficiary", beneficiaryKeyPair.public)
    val issuingBank = Party("issuingBank", issuingBankKeyPair.public)
    val advisingBank = Party("advisingBank", advisingBankKeyPair.public)
    val applicant = Party("applicant", applicantKeyPair.public)
    val otherplayerwedontcare = Party("osef", otherplayerwedontcareKeyPair.public)


    fun generateKeyPair(): KeyPair = Crypto.generateKeyPair()

    @Deprecated("Full legal names should be specified in all configurations")
    fun getPartyX509Name(commonName: String, location: String): X500Name {
        val nameBuilder = X500NameBuilder(BCStyle.INSTANCE)
        nameBuilder.addRDN(BCStyle.CN, commonName)
        nameBuilder.addRDN(BCStyle.O, "R3")
        nameBuilder.addRDN(BCStyle.OU, "corda")
        nameBuilder.addRDN(BCStyle.L, "London")
        nameBuilder.addRDN(BCStyle.C, location)
        return nameBuilder.build()
    }

    fun getDefaultSmartLC(): SmartLC.State = SmartLC.State(
            owner = beneficiaryKeyPair.public,
            beneficiary = beneficiary,
            issuingBank = issuingBank,
            advisingBank = advisingBank,
            applicant = applicant
    )


    @Test
    fun smartlc_is_created_first_status_is_draft_saved() {
        val inState = getDefaultSmartLC()

        ledger {
            transaction {
                input(inState)
                command(beneficiaryKeyPair.public) { SmartLC.Commands.Create() }
                this.verifies()
            }
        }
    }

    @Test
    fun smatlc_can_only_be_created_by_beneficiary() {
        val inState = getDefaultSmartLC()
        ledger {
            transaction {
                input(inState `with new owner` otherplayerwedontcareKeyPair.public)
                command(beneficiaryKeyPair.public) { SmartLC.Commands.Create() }
                this `fails with` "the transaction is created by beneficiary"
            }
        }
    }

    @Test
    fun smartlc_is_created_first_status_should_failed_if_not_draft_saved() {
        val inState = getDefaultSmartLC()
        inState.status = SmartLCStatus.ISSUED
        ledger {
            transaction {
                input(inState)
                command(beneficiaryKeyPair.public) { SmartLC.Commands.Create() }
                this `fails with` "the transaction is created only with status DRAFT"
            }
        }
    }

    @Test
    fun the_issuingBank_validate_the_transaction() {
        var inState = getDefaultSmartLC()
        inState = inState.`with new owner`(issuingBankKeyPair.public) as SmartLC.State
        ledger {
            transaction {
                input(inState `approved by` (issuingBank))
                command(issuingBankKeyPair.public) { SmartLC.Commands.Approve() }
                this.verifies()
            }
        }
    }

    @Test
    fun the_advisingBank_cannot_validate_the_transaction_directly() {
        var inState = getDefaultSmartLC()
        inState = inState.`with new owner`(advisingBankKeyPair.public) as SmartLC.State
        ledger {
            transaction {
                input(inState.`approved by`(advisingBank))
                command(advisingBankKeyPair.public) { SmartLC.Commands.Approve() }
                this `fails with` "the transaction cannot be approved"
            }
        }
    }

    @Test
    fun the_advisingBank_can_validate_the_transaction_only_after_issuingBank() {
        var inState = getDefaultSmartLC()
        inState = inState.`with new owner`(advisingBankKeyPair.public) as SmartLC.State
        inState = inState.`approved by`(issuingBank) as SmartLC.State
        ledger {
            transaction {
                input(inState.`approved by`(advisingBank))
                command(advisingBankKeyPair.public) { SmartLC.Commands.Approve() }
                this.verifies()
            }
        }
    }

    class CreateSmartOverFlowTest : SmartLCRealTest() {

        lateinit var net: MockNetwork
        lateinit var a: MockNetwork.MockNode
        lateinit var b: MockNetwork.MockNode
        lateinit var c: MockNetwork.MockNode
        lateinit var d: MockNetwork.MockNode
        lateinit var notary: Party

        @Before
        fun setup() {
            net = MockNetwork()
            val nodes = net.createSomeNodes(4)
            a = nodes.partyNodes[0]
            b = nodes.partyNodes[1]
            c = nodes.partyNodes[2]
            d = nodes.partyNodes[3]
            notary = nodes.notaryNode.info.notaryIdentity
            net.runNetwork()
        }

        @Test
        fun `resolve from two hashes`() {
            val stx1 = makeTransactions()
            val p = ResolveTransactionsFlow(setOf(stx1.id), a.info.legalIdentity)
            val future = b.services.startFlow(p).resultFuture
            net.runNetwork()
            val results = future.getOrThrow()
            assertEquals(listOf(stx1.id), results.map { it.id })
            b.database.transaction {
                assertEquals(stx1, b.storage.validatedTransactions.getTransaction(stx1.id))
            }
        }

        private fun makeTransactions(signFirstTX: Boolean = true, withAttachment: SecureHash? = null): SignedTransaction {

            // Make a chain of custody of dummy states and insert into node A.
            val createTx: SignedTransaction = SmartLC().generateCreate(a.info.legalIdentity.owningKey, a.info.legalIdentity, b.info.legalIdentity, c.info.legalIdentity, d.info.legalIdentity, DUMMY_NOTARY).let {
                if (withAttachment != null)
                    it.addAttachment(withAttachment)
                if (signFirstTX)
                    it.signWith(a.keyManagement.toKeyPair(a.info.legalIdentity.owningKey))
                it.signWith(DUMMY_NOTARY_KEY)
                it.toSignedTransaction(false)
            }

            a.database.transaction {
                a.services.recordTransactions(createTx)
            }

            return createTx
        }

        @After
        fun tearDown() {
            net.stopNodes()
        }
    }

}