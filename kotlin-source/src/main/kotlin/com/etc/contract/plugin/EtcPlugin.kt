package com.etc.contract.plugin

import com.etc.contract.status.SmartLCStatus
import net.corda.core.node.CordaPluginRegistry
import net.corda.core.serialization.SerializationCustomization

class EtcPlugin :  CordaPluginRegistry(){
    override fun customizeSerialization(custom: SerializationCustomization): Boolean {
        custom.addToWhitelist(SmartLCStatus::class.java)
        return true
    }
}

