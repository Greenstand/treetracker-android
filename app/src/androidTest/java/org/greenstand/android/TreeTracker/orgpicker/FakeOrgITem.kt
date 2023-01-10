package org.greenstand.android.TreeTracker.orgpicker

import org.greenstand.android.TreeTracker.models.organization.Destination
import org.greenstand.android.TreeTracker.models.organization.Org

object FakeOrgITem {

    val orgitem =  Org(
        id = "123",
        name = "BESTORG",
        walletId = "abc",
        logoPath = "path",
        mutableListOf(Destination(
            "hello",
            mutableListOf("hhh")
        )),
        mutableListOf(Destination(
            "hello",
            mutableListOf("hh")
        ))
    )
}