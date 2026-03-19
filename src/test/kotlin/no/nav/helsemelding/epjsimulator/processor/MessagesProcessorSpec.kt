package no.nav.helsemelding.epjsimulator.processor

import arrow.core.Either.Left
import arrow.core.Either.Right
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import no.nav.helsemelding.ediadapter.model.ErrorMessage
import no.nav.helsemelding.ediadapter.model.Message
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

        "should return true if outgoing message is processed successfully" {
            val ediAdapterClient = FakeEdiAdapterClient()

            val message = Message(
                id = Uuid.random(),
                isAppRec = false
            )

            ediAdapterClient.givenPostApprec(message.id!!, Right(Metadata(Uuid.random(), "")))
            ediAdapterClient.givenMarkAsRead(message.id!!, Right(true))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.processMessage(message) shouldBe true
        }

        "should return true if outgoing apprec is processed successfully" {
            val ediAdapterClient = FakeEdiAdapterClient()

            val message = Message(
                id = Uuid.random(),
                isAppRec = true
            )

            ediAdapterClient.givenMarkAsRead(message.id!!, Right(true))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.processMessage(message) shouldBe true
        }

        "should return false if sending apprec fails" {
            val ediAdapterClient = FakeEdiAdapterClient()

            val message = Message(
                id = Uuid.random(),
                isAppRec = false
            )

            ediAdapterClient.givenPostApprec(message.id!!, Left(errorMessage404))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.processMessage(message) shouldBe false
        }

        "should return false if marking message as read fails" {
            val ediAdapterClient = FakeEdiAdapterClient()

            val message = Message(
                id = Uuid.random(),
                isAppRec = true
            )

            ediAdapterClient.givenPostApprec(message.id!!, Right(Metadata(Uuid.random(), "")))
            ediAdapterClient.givenMarkAsRead(message.id!!, Left(errorMessage404))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.processMessage(message) shouldBe false
        }

        "should return false if marking apprec as read fails" {
            val ediAdapterClient = FakeEdiAdapterClient()

            val message = Message(
                id = Uuid.random(),
                isAppRec = true
            )

            ediAdapterClient.givenMarkAsRead(message.id!!, Left(errorMessage404))

            val messagesProcessor = MessagesProcessor(ediAdapterClient)

            messagesProcessor.processMessage(message) shouldBe false
        }
    }
)
