package com.etc.contract.plugin

import com.etc.contract.status.SmartLCStatus
import com.etc.flow.CreateSmartLcFlow
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.node.PluginServiceHub
import net.corda.core.serialization.SerializationCustomization
import java.util.function.Function

class EtcPlugin constructor(
        override val webApis: List<Function<CordaRPCOps, out Any>> = emptyList(),
        override val staticServeDirs: Map<String, String> = emptyMap(),
        override val requiredFlows: Map<String, Set<String>> = mapOf(
                "com.etc.flow.CreateSmartLcFlow" to setOf("net.i2p.crypto.eddsa.EdDSAPublicKey", "net.corda.core.crypto.Party")
        )
        ,
        override val servicePlugins: List<Function<PluginServiceHub, out Any>> = emptyList()
) : CordaPluginRegistry() {


    //override var requiredFlows: Map<String, Set<String>> = emptyMap()

    override fun customizeSerialization(custom: SerializationCustomization): Boolean {

        custom.addToWhitelist(SmartLCStatus::class.java)
        custom.addToWhitelist(CreateSmartLcFlow::class.java)
        return true
    }
}

