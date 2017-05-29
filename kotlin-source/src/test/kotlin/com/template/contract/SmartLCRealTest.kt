package com.template.contract

import com.etc.contract.*
import com.etc.contract.status.SmartLCStatus
import net.corda.core.utilities.DUMMY_NOTARY
import net.corda.core.crypto.Crypto
import net.corda.core.crypto.Party
import net.corda.core.transactions.TransactionBuilder
import net.corda.testing.ledger
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.asn1.x500.X500NameBuilder
import org.bouncycastle.asn1.x500.style.BCStyle
import org.junit.Test
import java.security.KeyPair


class SmartLCRealTest {

    val beneficiaryKeyPair = generateKeyPair()
    val issuingBankKeyPair = generateKeyPair()
    val advisingBankKeyPair = generateKeyPair()
    val applicantKeyPair = generateKeyPair()
    val otherplayerwedontcareKeyPair = generateKeyPair()



    val beneficiary = Party("beneficiary", beneficiaryKeyPair.public).ref(4)
    val issuingBank = Party("issuingBank", issuingBankKeyPair.public).ref(1)
    val advisingBank = Party("advisingBank", advisingBankKeyPair.public).ref(2)
    val applicant = Party("applicant", applicantKeyPair.public).ref(3)
    val otherplayerwedontcare = Party("osef", otherplayerwedontcareKeyPair.public).ref(4)


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
       inState = inState.`approved by`(issuingBank)as SmartLC.State
       ledger {
           transaction {
               input(inState.`approved by`(advisingBank))
               command(advisingBankKeyPair.public) { SmartLC.Commands.Approve() }
               this.verifies()
           }
       }
   }

    @Test
    fun test_howwork_api(){
        var inState = SmartLC()
        ledger{
            inState.generateCreate(beneficiaryKeyPair.public,beneficiary,issuingBank,advisingBank,applicant,DUMMY_NOTARY)
            this.verifies()
        }
    }


}