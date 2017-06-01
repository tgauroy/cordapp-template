package com.etc.contract

import net.corda.core.contracts.*
import net.corda.core.crypto.NullPublicKey
import net.corda.core.crypto.SecureHash
import java.security.PublicKey
import java.time.Instant
import java.util.*
import com.etc.contract.status.*
import net.corda.core.crypto.Party
import net.corda.core.transactions.TransactionBuilder


open class SmartLC : Contract {

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Approve : TypeOnlyCommandData(), Commands
        class Move : TypeOnlyCommandData(), Commands
    }

    override val legalContractReference: SecureHash
            = SecureHash.sha256("https://en.wikipedia.org/wiki/Letter_of_credit")

    override fun verify(tx: TransactionForContract) {
        // Group by everything except owner: any modification to the CP at all is considered changing it fundamentally.
        val groups = tx.groupStates() { it: State -> it.withoutOwner() }

        val command = tx.commands.requireSingleCommand<Commands>()

        val timestamp: Timestamp? = tx.timestamp

        for ((inputs, outputs, key) in groups) {
            when (command.value) {

                is Commands.Move -> {
                    val output = outputs.single()
                    requireThat {
                        "the state is propagated" using (outputs.size == 1)
                    }
                }

                is Commands.Create -> {
                    val output = outputs.single()
                    requireThat {
                        "the transaction is created only with status DRAFT" using (SmartLCStatus.DRAFT_APPROUVED == output.status)
                        "the transaction is created by beneficiary" using (command.signers.single() == output.owner)
                    }
                }

                is Commands.Approve -> {
                    val output = outputs.single()
                    requireThat {
                        "the transaction cannot be approved" using ((SmartLCStatus.ISSUED == output.status || SmartLCStatus.ISSUANCE_ACCEPTED == output.status)
                                && (command.signers.single() == output.owner))
                    }
                }
                else -> throw IllegalArgumentException("Unrecognised command")
            }
        }
    }

    data class State(

            override val owner: PublicKey,

            var applicant: Party? = null,
            val beneficiary: Party? = null,

            val issuingBank: Party? = null,
            val advisingBank: Party? = null,
            val negotiatingBank: Party? = null,

            val ETCReferenceID: String? = null,


            var issuingBankValidated: Boolean? = false,
            var advisingBankValidated: Boolean? = false,


            val priceFixed: Amount<Issued<Currency>>? = null,
            val goodsDescription: String? = null,
            val origin: String? = null,

            val quantity: String? = null,
            val quantityUnit: String? = null,
            val commodityTolerance: Integer? = null,
            val priceUnit: Currency? = null,
            val isPriceFixed: Boolean? = null,
            val floatingFormula: String? = null,
            val pricePer: Amount<Issued<Currency>>? = null,
            val qualities: Arrays? = null,

            val incoterm: Integer? = null,
            val incotermPlace: String? = null,
            val transshipment: Boolean? = null,
            val partialShipment: Boolean? = null,

            val portOfLanding: String? = null,
            val portOfDischarge: String? = null,

            val shippingDateFrom: Instant? = null,
            val shippingDateTo: Instant? = null,

            val transactionCurrency: Currency? = null,
            val totalAmountTolerance: Integer? = null,
            val totalAmount: Amount<Issued<Currency>>? = null,

            val expiryDate: Instant? = null,
            val noOfdaysForPayment: Integer? = null,
            val periodOfPresentation: Integer? = null,

            val outcomeEndorsement: String? = null,

            // docs
            val paymentAfter: Boolean? = null,
            val billOfLading: Boolean? = null,
            val certificateOfQuality: Boolean? = null,
            val certificateOfQuantity: Boolean? = null,
            val certificateOfOrigin: Boolean? = null,
            val invoice: Boolean? = null,

            val fluctuationClause: Boolean? = null,

            var status: SmartLCStatus? = SmartLCStatus.DRAFT_APPROUVED


    ) : OwnableState {

        override fun withNewOwner(newOwner: PublicKey) = Pair(SmartLC.Commands.Move(), copy(owner = newOwner))

        override val contract = SmartLC()
        override val participants = listOf(owner)

        fun withoutOwner() = copy(owner = NullPublicKey)
        fun withOwner(newOwner: PublicKey): ContractState = copy(owner = newOwner)
        fun approveFor(approver: Party): ContractState {
            var statusToPromoted = SmartLCStatus.DRAFT_APPROUVED

            if (approver == issuingBank) {
                statusToPromoted = SmartLCStatus.ISSUED
                return copy(status = statusToPromoted, issuingBankValidated = true)
            }

            if (approver == advisingBank && issuingBankValidated as Boolean) {
                statusToPromoted = SmartLCStatus.ISSUANCE_ACCEPTED
                return copy(status = statusToPromoted, advisingBankValidated = true)
            }
            return copy(status = statusToPromoted)
        }

    }

    fun generateCreate(owner: PublicKey, beneficiary: Party, issuingBank: Party, advisingBank: Party, applicant: Party, notary: Party): TransactionBuilder {
        val state = State(owner, beneficiary, issuingBank, advisingBank, applicant)
        return TransactionBuilder(notary = notary).withItems(state, Command(Commands.Create(), beneficiary.owningKey))
    }

    fun generateApprove(tx: TransactionBuilder, smartlc: StateAndRef<State>, newOwner: PublicKey, approver: Party) {
        tx.addInputState(smartlc)
        tx.addOutputState(smartlc.state.data.withOwner(newOwner))
        tx.addOutputState(smartlc.state.data.approveFor(approver))
        tx.addCommand(Command(Commands.Approve(), smartlc.state.data.owner))
    }
}

fun SmartLC.State.changeOwner(newOwner: PublicKey): ContractState = copy(owner = newOwner)

fun SmartLC.State.approveSmartLc(approver: Party): ContractState {
    var statusToPromoted = SmartLCStatus.DRAFT_APPROUVED

    if (approver == issuingBank) {
        statusToPromoted = SmartLCStatus.ISSUED
        return copy(status = statusToPromoted, issuingBankValidated = true)
    }

    if (approver == advisingBank && issuingBankValidated as Boolean) {
        statusToPromoted = SmartLCStatus.ISSUANCE_ACCEPTED
        return copy(status = statusToPromoted, advisingBankValidated = true)
    }
    return copy(status = statusToPromoted)
}

fun SmartLC.State.getApprovalSate(approver: Party): Boolean {
    if (approver == issuingBank) {
        return issuingBankValidated as Boolean
    } else if (approver == advisingBank) {
        return advisingBankValidated as Boolean
    }
    return false
}


infix fun SmartLC.State.`approved by`(approbator: Party): ContractState = approveSmartLc(approbator)
infix fun SmartLC.State.`with new owner`(newowner: PublicKey): ContractState = changeOwner(newowner)

infix fun SmartLC.State.`is approved by`(approbator: Party): Boolean = getApprovalSate(approbator)











