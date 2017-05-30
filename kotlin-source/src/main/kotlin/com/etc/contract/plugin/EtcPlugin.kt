package com.etc.contract.plugin

import com.etc.contract.status.SmartLCStatus
import com.etc.flow.CreateSmartLcFlow
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.serialization.SerializationCustomization

class EtcPlugin : CordaPluginRegistry() {

    override fun customizeSerialization(custom: SerializationCustomization): Boolean {
        custom.addToWhitelist(SmartLCStatus::class.java)
        custom.addToWhitelist(CreateSmartLcFlow::class.java)
        return true
    }
}

