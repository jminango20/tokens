package com.bikemarket.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.MoveNonFungibleTokensHandler
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.types.PartyAndToken
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker
import com.bikemarket.states.FrameTokenState
import com.bikemarket.states.WheelsTokenState

// *********
// * Flows *
// *********
@InitiatingFlow
@StartableByRPC
class TransferPartToken(private val part: String,
                        private val serial: String,
                        private val holder: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        when (part) {
            "frame" -> {
            val frameSerial = serial

            val frameStateAndRef = serviceHub.vaultService.queryBy<FrameTokenState>().states
                    .filter { it.state.data.serialNum == frameSerial }[0]


            val frametokentype = frameStateAndRef.state.data


            val frametokenPointer = frametokentype.toPointer(frametokentype.javaClass)
            val partyAndFrameToken = PartyAndToken(holder,frametokenPointer)

            val stx = subFlow(MoveNonFungibleTokens(partyAndFrameToken))

                return ("Transfere a propriedade do frame token com serial: " + this.serial + " para"
                        + holder.name.organisation)
        }
            "wheels" -> {
            val wheelsSerial = serial

            val wheelStateAndRef = serviceHub.vaultService.queryBy<WheelsTokenState>().states
                    .filter { it.state.data.serialNum == wheelsSerial }[0]

            //get the TokenType object
            val wheeltokentype: WheelsTokenState = wheelStateAndRef.state.data

            //get the pointer pointer to the wheel
            val wheeltokenPointer = wheeltokentype.toPointer(wheeltokentype.javaClass)
            val partyAndWheelToken = PartyAndToken(holder, wheeltokenPointer)

            val stx = subFlow(MoveNonFungibleTokens(partyAndWheelToken))

                return "Transfere a propriedade do wheel token com serial: $serial para ${holder.name.organisation}"
        }
            else -> throw FlowException("Nao foi ingressado nenhum frame nem wheels")
        }
    }
}

@InitiatedBy(TransferPartToken::class)
class TransferPartTokenResponder(private val counterpartySession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        // Responder flow logic goes here.
        subFlow(MoveNonFungibleTokensHandler(counterpartySession));
    }
}

