package com.etc.contract

import net.corda.core.contracts.*
import net.corda.core.crypto.NullPublicKey
import net.corda.core.crypto.SecureHash
import java.security.PublicKey
import java.time.Instant
import java.util.*
import com.etc.contract.status.*
import net.corda.core.transactions.TransactionBuilder


open class SmartLC : Contract {

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Approve : TypeOnlyCommandData(), Commands
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

                is Commands.Create -> {
                    val input = inputs.single()
                    requireThat {
                        "the transaction is created with status DRAFT" using (SmartLCStatus.DRAFT_APPROUVED == input.status)
                        "only a trader can create a new contract" using (input.applicant == input.beneficiary)
                    }
                }

                is Commands.Approve -> {
                    val input = inputs.single()
                    requireThat {
                        "the transaction is approved with status ISSUED" using (input.applicant == input.issuingBank)
                        "the transaction is approved with status ISSUANCE ACCEPTED" using (input.applicant == input.advisingBank)

                    }
                }


                else -> throw IllegalArgumentException("Unrecognised command")
            }
        }
    }

    data class State(
            val issuance: PartyAndReference,
            override val owner: PublicKey,

            val ETCReferenceID: String? = null,

            var applicant: PartyAndReference? = null,
            val beneficiary: PartyAndReference? = null,

            val issuingBank: PartyAndReference? = null,
            val advisingBank: PartyAndReference? = null,
            val negotiatingBank: PartyAndReference? = null,

            var issuingBankValidation: Boolean? = false,
            var advisingBankValidation: Boolean? = false,


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
        override fun withNewOwner(newOwner: PublicKey): Pair<CommandData, OwnableState> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override val contract = SmartLC()
        override val participants = listOf(owner)

        fun withoutOwner() = copy(owner = NullPublicKey)

        fun approveContract(tx : TransactionBuilder, contract: StateAndRef<State>, approver: PublicKey) {
            tx.addInputState(contract)
            tx.addOutputState(TransactionState(contract.state.data.copy(owner = approver), contract.state.notary))
            tx.addOutputState(TransactionState(contract.state.data.apply {status = SmartLCStatus.ISSUED }, contract.state.notary))
            tx.addCommand(Commands.Approve())
        }

    }


}







