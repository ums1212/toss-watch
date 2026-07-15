package dev.comon.toss_watch.feature.dashboard.data.remote.dto

import dev.comon.toss_watch.feature.dashboard.domain.model.Account
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AccountResponse(
    @SerialName("accountNo") val accountNo: String,
    @SerialName("accountSeq") val accountSeq: Long,
    @SerialName("accountType") val accountType: String,
)

fun AccountResponse.toAccount(): Account =
    Account(
        accountNo = accountNo,
        accountSeq = accountSeq,
        accountType = accountType,
    )
