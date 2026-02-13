package no.nav.helsemelding.epjsimulator.processor

import arrow.core.Either.Left
import arrow.core.Either.Right
import io.kotest.core.spec.style.StringSpec
import io.kotest.data.forAll
import io.kotest.data.row
import io.kotest.matchers.shouldBe
import no.nav.helsemelding.ediadapter.model.ErrorMessage
import no.nav.helsemelding.ediadapter.model.Metadata
import no.nav.helsemelding.epjsimulator.util.FakeEdiAdapterClient
import kotlin.uuid.Uuid

class MessagesProcessorSpec : StringSpec(
    {
        val errorMessage404 = ErrorMessage(
            error = "Not Found",
            errorCode = 1000,
            requestId = Uuid.random().toString()
        )

        "should return true if sending apprec and marking message and apprec as read succeeds" {
            val ediAdapterClient = FakeEdiAdapterClient()
            val messageId = Uuid.random()
            val apprecId = Uuid.random()
            ediAdapterClient.givenPostApprec(messageId, Right(Metadata(apprecId, "")))
            ediAdapterClient.givenMarkAsRead(messageId, Right(true))
            ediAdapterClient.givenMarkAsRead(apprecId, Right(true))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.sendApprecAndMarkMessageAsRead(messageId) shouldBe true
        }

        "should return false if sending apprec succeeds and marking message and/or apprec as read fails" {
            forAll(
                row(Left(errorMessage404), Left(errorMessage404)),
                row(Right(true), Left(errorMessage404)),
                row(Left(errorMessage404), Right(true))
            ) { messageResponse, apprecResponse ->
                val ediAdapterClient = FakeEdiAdapterClient()
                val messageId = Uuid.random()
                val apprecId = Uuid.random()
                ediAdapterClient.givenPostApprec(messageId, Right(Metadata(apprecId, "")))
                ediAdapterClient.givenMarkAsRead(messageId, messageResponse)
                ediAdapterClient.givenMarkAsRead(apprecId, apprecResponse)

                val messagesProcessor = MessagesProcessor(ediAdapterClient)

                messagesProcessor.sendApprecAndMarkMessageAsRead(messageId) shouldBe false
            }
        }

        "should return false if sending apprec fails" {
            val ediAdapterClient = FakeEdiAdapterClient()
            val uuid = Uuid.random()
            ediAdapterClient.givenPostApprec(uuid, Left(errorMessage404))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.sendApprecAndMarkMessageAsRead(uuid) shouldBe false
        }
    }
)
