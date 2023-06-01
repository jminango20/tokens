package com.bikemarket.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenPointer
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldBy
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.ProgressTracker
import com.bikemarket.states.FrameTokenState
import com.bikemarket.states.WheelsTokenState

// *********
// * Flows *
// *********
@StartableByRPC
class IssueNewBike(private val frameSerial: String,
                   private val wheelsSerial: String,
                   private val holder: Party) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {
        /*PARTE 1 do FRAME TOKEN */
        //Passo 1: Frame Token
        //pegando frame states na ledger
        val frameStateAndRef = serviceHub.vaultService.queryBy<FrameTokenState>().states
                .filter { it.state.data.serialNum == (frameSerial) }[0]

        //Objeto TokenType
        val frametokentype = frameStateAndRef.state.data

        //Passo 2: vamos obter o pointer para o frame
        val frametokenPointer = frametokentype.toPointer(frametokentype.javaClass)

        //Passo 3: Colocar o issuer (emissor) do frame toke type para quem estou emitindo os tokens.
        val frameissuedTokenType = frametokenPointer issuedBy ourIdentity

        //Passo 4: Mencionar quem e o dono atual do frame token
        val frametoken = frameissuedTokenType heldBy holder

        /*PARTE 2 do WHEELS TOKEN */
        val wheelStateAndRef = serviceHub.vaultService.queryBy<WheelsTokenState>().states
                .filter { it.state.data.serialNum == wheelsSerial }[0]


        val wheeltokentype: WheelsTokenState = wheelStateAndRef.state.data


        val wheeltokenPointer: TokenPointer<*> = wheeltokentype.toPointer(wheeltokentype.javaClass)


        val wheelissuedTokenType = wheeltokenPointer issuedBy ourIdentity


        val wheeltoken = wheelissuedTokenType heldBy holder


        val stx = subFlow(IssueTokens(listOf(frametoken,wheeltoken)))

        return ("A nova bicicleta foi emitida para " + holder.name.organisation + "com frame serial: "
                + this.frameSerial + " wheels serial: " + this.wheelsSerial)
    }
}