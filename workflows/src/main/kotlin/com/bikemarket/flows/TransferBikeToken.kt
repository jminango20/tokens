
package com.bikemarket.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import net.corda.core.flows.*
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import com.bikemarket.states.FrameTokenState
import com.bikemarket.states.WheelsTokenState

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TransferBikeToken(private val frameSerial: String,
                        private val wheelsSerial: String,
                        private val holder: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        /*FRAME*/
        val frameStateAndRef = serviceHub.vaultService.queryBy<FrameTokenState>().states
                .filter { it.state.data.serialNum == (frameSerial) }[0]

        //Objeto TokenType
        val frametokentype = frameStateAndRef.state.data

        //Obter o pointer do frame
        val frametokenPointer: TokenPointer<*> = frametokentype.toPointer(frametokentype.javaClass)

        /*WHEELS*/
        val wheelStateAndRef = serviceHub.vaultService.queryBy<WheelsTokenState>().states
                .filter { it.state.data.serialNum == (wheelsSerial) }[0]


        val wheeltokentype: WheelsTokenState = wheelStateAndRef.state.data


        val wheeltokenPointer: TokenPointer<*> = wheeltokentype.toPointer(wheeltokentype.javaClass)


        val session = initiateFlow(holder)

        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Sao Paulo,C=BR"))
        val txBuilder = TransactionBuilder(notary)
        addMoveNonFungibleTokens(txBuilder,serviceHub,frametokenPointer,holder)
        addMoveNonFungibleTokens(txBuilder,serviceHub,wheeltokenPointer,holder)
        val ptx = serviceHub.signInitialTransaction(txBuilder)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(session)))
        val ftx = subFlow(ObserverAwareFinalityFlow(stx, listOf(session)))


        return "Transferencia da bicicleta com Frame serial $frameSerial e Wheels serial $wheelsSerial para ${holder.name.organisation}"

    }
}

@InitiatedBy(TransferBikeToken::class)
class TransferBikeTokenResponder(private val flowSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call(): Unit {
        subFlow(ObserverAwareFinalityFlowHandler(flowSession))
    }
}

