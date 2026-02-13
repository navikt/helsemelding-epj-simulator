package no.nav.helsemelding.epjsimulator.processor

import arrow.core.Either.Left
import arrow.core.Either.Right
import io.kotest.core.spec.style.StringSpec
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

        "should return true if sending apprec and marking message as read succeeds" {
            val ediAdapterClient = FakeEdiAdapterClient()
            val uuid = Uuid.random()
            ediAdapterClient.givenPostApprec(uuid, Right(Metadata(Uuid.random(), "")))
            ediAdapterClient.givenMarkAsRead(uuid, Right(true))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.sendApprecAndMarkMessageAsRead(uuid) shouldBe true
        }

        "should return false if sending apprec succeeds and marking message as read fails" {
            val ediAdapterClient = FakeEdiAdapterClient()
            val uuid = Uuid.random()
            ediAdapterClient.givenPostApprec(uuid, Right(Metadata(Uuid.random(), "")))
            ediAdapterClient.givenMarkAsRead(uuid, Left(errorMessage404))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.sendApprecAndMarkMessageAsRead(uuid) shouldBe false
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
