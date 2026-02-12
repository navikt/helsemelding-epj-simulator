package no.nav.helsemelding.epjsimulator.processor

import arrow.core.Either
import arrow.core.Either.Left
import arrow.core.Either.Right
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import no.nav.helsemelding.ediadapter.client.EdiAdapterClient
import no.nav.helsemelding.ediadapter.model.AppRecError
import no.nav.helsemelding.ediadapter.model.AppRecStatus
import no.nav.helsemelding.ediadapter.model.ErrorMessage
import no.nav.helsemelding.ediadapter.model.GetMessagesRequest
import no.nav.helsemelding.ediadapter.model.Metadata
import no.nav.helsemelding.ediadapter.model.PostAppRecRequest
import kotlin.uuid.Uuid

private val log = KotlinLogging.logger {}

const val ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1_HERID = 8142519

class MessagesProcessor(
    private val ediAdapterClient: EdiAdapterClient
) {

    suspend fun processMessages(scope: CoroutineScope) =
        messageFlow()
            .onEach(::sendApprecAndMarkMessageAsRead)
            .flowOn(Dispatchers.IO)
            .launchIn(scope)

    private suspend fun messageFlow(): Flow<Uuid> {
        val getMessagesRequest = GetMessagesRequest(
            receiverHerIds = listOf(ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1_HERID),
            includeMetadata = true
        )

        return when (val messages = ediAdapterClient.getMessages(getMessagesRequest)) {
            is Right ->
                messages.value
                    .filter { it.isAppRec == false }
                    .filter { it.id != null }
                    .map { it.id as Uuid }
                    .asFlow()

            is Left -> emptyFlow()
        }
    }

    internal suspend fun sendApprecAndMarkMessageAsRead(messageId: Uuid): Boolean {
        return when (val either = postApprec(messageId)) {
            is Right<Metadata> -> {
                log.info { "Successfully posted apprec for message: $messageId which received the following apprecId: ${either.value.id}" }
                markMessageAsRead(messageId)
            }

            is Left<ErrorMessage> -> {
                log.error { "Failed to send apprec for message: $messageId. Error: ${either.value}" }
                false
            }
        }
    }

    private suspend fun postApprec(messageId: Uuid): Either<ErrorMessage, Metadata> {
        val postAppRecRequest = postAppRecRequests.random()
        log.info { "Attempting to post apprec for message: $messageId with the following apprecStatus: ${postAppRecRequest.appRecStatus}" }

        return ediAdapterClient.postApprec(
            id = messageId,
            apprecSenderHerId = ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1_HERID,
            postAppRecRequest = postAppRecRequest
        )
    }

    private suspend fun markMessageAsRead(messageId: Uuid): Boolean {
        val either = ediAdapterClient.markMessageAsRead(messageId, ADRESSEREGISTERET_HELSEOPPLYSNINGER_TEST1_HERID)

        return when (either) {
            is Right<Boolean> -> {
                log.info { "Successfully marked message: $messageId as read." }
                either.value
            }

            is Left<ErrorMessage> -> {
                log.error { "Failed to mark message: $messageId as read. Error: ${either.value}" }
                false
            }
        }
    }
}

val apprecOk = PostAppRecRequest(
    appRecStatus = AppRecStatus.OK
)

val apprecRejected = PostAppRecRequest(
    appRecStatus = AppRecStatus.REJECTED,
    appRecErrorList = listOf(
        AppRecError(
            errorCode = "E100",
            description = "Could not parse business document"
        )
    )
)

val apprecErrorMessageInPart = PostAppRecRequest(
    appRecStatus = AppRecStatus.OK_ERROR_IN_MESSAGE_PART,
    appRecErrorList = listOf(
        AppRecError(
            errorCode = "E123",
            description = "Invalid document structure"
        )
    )
)

val postAppRecRequests = listOf(
    apprecOk,
    apprecRejected,
    apprecErrorMessageInPart
)
