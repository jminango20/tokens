package com.bikemarket.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.ProgressTracker
import com.bikemarket.states.FrameTokenState

// *********
// * Flows *
// *********
@StartableByRPC
class CreateFrameToken(private val frameSerial: String) : FlowLogic<String>() {
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call():String {

        //Obter uma referencia para o notary que desejo utilizar
        val notary = serviceHub.networkMapCache.getNotary(CordaX500Name.parse("O=Notary,L=Sao Paulo,C=BR"))

        //Criar NFT do frame token
        val uuid = UniqueIdentifier()
        val frame = FrameTokenState(ourIdentity, uuid,0,frameSerial)

        //A transacao esteja linkada a um notary
        val transactionState = frame withNotary notary!!

        subFlow(CreateEvolvableTokens(transactionState))

        return "Criado um frame token para bicicleta com Serial Number ${this.frameSerial}."
    }
}
