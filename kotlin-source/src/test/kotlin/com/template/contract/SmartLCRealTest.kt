package com.template.contract

import com.etc.contract.SmartLC
import com.etc.contract.status.SmartLCStatus
import net.corda.core.contracts.DOLLARS

import net.corda.core.contracts.`issued by`
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.Party
import net.corda.core.days
import net.corda.core.utilities.TEST_TX_TIME
import net.corda.testing.ledger
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.junit.Test
import java.security.KeyPair


class SmartLCRealTest {

    val currentKEyPairMyCorp = generateKeyPair()
    val currentKEyPairMyNewOwner = generateKeyPair()

    val IssuingBank = Party("IssuingBank", currentKEyPairMyNewOwner.public).ref(1)
    val advisingBank = Party("advisingBank", currentKEyPairMyNewOwner.public).ref(2)
    val applicant = Party("applicant", currentKEyPairMyNewOwner.public).ref(3)
    val beneficiary = Party("beneficiary", currentKEyPairMyNewOwner.public).ref(4)
    val otherplayerwedontcare = Party("osef", currentKEyPairMyNewOwner.public).ref(4)

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
            issuance = beneficiary,
            owner = currentKEyPairMyCorp.public
    )


    @Test
    fun when_smartlc_is_created_first_status_is_draft_saved() {
        val inState = getDefaultSmartLC()

        ledger {
            transaction {
                input(inState)
                command(currentKEyPairMyCorp.public) { SmartLC.Commands.Create() }
                this.verifies()
            }
        }
    }

    @Test
    fun when_smartlc_is_created_first_status_should_failed_if_not_draft_saved() {
        val inState = getDefaultSmartLC()
        inState.status = SmartLCStatus.ISSUED
        ledger {
            transaction {
                input(inState)
                command(currentKEyPairMyCorp.public) { SmartLC.Commands.Create() }
                this `fails with` "the transaction is created with status DRAFT"
            }
        }
    }

    @Test
    fun only_trader_can_create_a_contract() {
        val inState = getDefaultSmartLC()
        inState.applicant = otherplayerwedontcare
        ledger {
            transaction {
                input(inState)
                command(currentKEyPairMyCorp.public) { SmartLC.Commands.Create() }
                this `fails with` "only a trader can create a new contract"
            }
        }
    }

    @Test
    fun simulate_issuing_bank_validation() {
        val inState = getDefaultSmartLC()
        ledger {
            transaction {
                input(inState)
                output("paper") { getDefaultSmartLC().approveContract() } // Some CP is issued onto the ledger by MegaCorp.
                command(currentKEyPairMyCorp.public) { SmartLC.Commands.Approve() }
                this `fails with` "output values sum to more than the inputs"
            }
        }
    }


}