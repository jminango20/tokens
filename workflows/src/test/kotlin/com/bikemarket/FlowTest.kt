package com.bikemarket

import com.bikemarket.flows.*
import com.bikemarket.states.FrameTokenState
import com.bikemarket.states.WheelsTokenState
import com.google.common.collect.ImmutableList
import net.corda.core.identity.CordaX500Name
import net.corda.testing.node.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FlowTest {
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var c: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(
                notarySpecs = listOf(MockNetworkNotarySpec(CordaX500Name("Notary","London","GB")))
        ).withCordappsForAllNodes(ImmutableList.of(
                TestCordapp.findCordapp("com.bikemarket.contracts"),
                TestCordapp.findCordapp("com.bikemarket.flows"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.contracts"),
                TestCordapp.findCordapp("com.r3.corda.lib.tokens.workflows"))))

        a = network.createPartyNode(null)
        b = network.createPartyNode(null)
        c = network.createPartyNode(null)
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun bikeTokensCreation() {
        val frameflow = CreateFrameToken("1234")
        a.startFlow(frameflow)
        network.runNetwork()

        val wheelflow = CreateWheelToken("abcd")
        a.startFlow(wheelflow)
        network.runNetwork()

        val frameInVault = a.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertNotNull(frameInVault)

        val wheelInVault = a.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertNotNull(wheelInVault)
    }

    @Test
    fun issueNewBike() {
        val frameflow = CreateFrameToken("1234")
        a.startFlow(frameflow)
        network.runNetwork()

        val wheelflow = CreateWheelToken("abcd")
        a.startFlow(wheelflow)
        network.runNetwork()

        val issueBikeFlow = IssueNewBike(frameSerial = "1234",
                     wheelsSerial = "abcd",
                     holder = b.info.legalIdentities.first())

        a.startFlow(issueBikeFlow)
        network.runNetwork()

        val frameInVault = b.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertNotNull(frameInVault)

        val wheelInVault = b.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertNotNull(wheelInVault)

        val frameInVaultA = a.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertEquals(frameInVaultA.size, 0)

        val wheelInVaultA = a.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertEquals(wheelInVaultA.size, 0)
    }

    @Test
    fun transferBike() {
        val frameflow = CreateFrameToken("1234")
        a.startFlow(frameflow)
        network.runNetwork()

        val wheelflow = CreateWheelToken("abcd")
        a.startFlow(wheelflow)
        network.runNetwork()

        val issueBikeFlow = IssueNewBike(frameSerial = "1234",
                     wheelsSerial = "abcd",
                     holder = b.info.legalIdentities.first())

        a.startFlow(issueBikeFlow)
        network.runNetwork()

        //TransferBikeFlow
        val transferBikeToken = TransferBikeToken(frameSerial = "1234",
                                                  wheelsSerial = "abcd",
                                                  holder = c.info.legalIdentities.first())
        b.startFlow(transferBikeToken) //como os dois NFTs foram emitidos ao node b, entao esse node e o unico de chamar ao flow de transferencia do NFTs
        network.runNetwork()

        //Vamos checar se os dois NFTs estao no vault do node C
        val frameInVault = c.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertNotNull(frameInVault)

        val wheelInVault = c.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertNotNull(wheelInVault)

        //Os NFTs nao devem estar nem no vault do node A e B
        val frameInVaultA = a.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertEquals(frameInVaultA.size, 0)

        val wheelInVaultA = a.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertEquals(wheelInVaultA.size, 0)

        val frameInVaultB = b.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertEquals(frameInVaultB.size, 0)

        val wheelInVaultB = b.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertEquals(wheelInVaultB.size, 0)
    }

    @Test
    fun transferPartToken() {
        val frameflow = CreateFrameToken("1234")
        a.startFlow(frameflow)
        network.runNetwork()

        val wheelflow = CreateWheelToken("abcd")
        a.startFlow(wheelflow)
        network.runNetwork()

        val issueBikeFlow = IssueNewBike(frameSerial = "1234",
                     wheelsSerial = "abcd",
                     holder = b.info.legalIdentities.first())

        a.startFlow(issueBikeFlow)
        network.runNetwork()

        //TransferPartFlow
        val transferBikeToken = TransferPartToken(part = "frame",
                                                  serial = "1234",
                                                  holder = c.info.legalIdentities.first())

        b.startFlow(transferBikeToken) //como os dois NFTs foram emitidos ao node b, entao esse node e o unico de chamar ao flow de transferencia do NFTs
        network.runNetwork()

        //Vamos checar que so o NFT do frame esta no vault do node C
        val frameInVault = c.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertNotNull(frameInVault)
        //No vault do node C nao deve existir nenhum estado WheelsTokenState pois so foi transferido o NFT do frame
        val wheelInVault = c.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertEquals(wheelInVault.size, 0)

        //Os NFTs nao devem estar nem no vault do node A e B
        val frameInVaultA = a.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertEquals(frameInVaultA.size, 0)

        val wheelInVaultA = a.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertEquals(wheelInVaultA.size, 0)

        val frameInVaultB = b.services.vaultService.queryBy(FrameTokenState::class.java).states
        assertEquals(frameInVaultB.size, 0)

        val wheelInVaultB = b.services.vaultService.queryBy(WheelsTokenState::class.java).states
        assertEquals(wheelInVaultB.size, 0)
    }
}