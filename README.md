# helsemelding-epj-simulator

Application for simulating behaviour of an EPJ (Elektronisk pasientjournal) system through EDI 2.0. 
The counterpart in this flow is [helsemelding-message-generator](https://github.com/navikt/helsemelding-message-generator) 
which is producing the messages this application will be consuming.

Overview:
- Poll for new messages sent (default is `10`)
- Send `apprec` for message(s)
- Update message(s) as `read`

## Local development

Running the application locally:
1. Replace the usage of `ediAdapterClient` with a fake one.
   See [Replacing messagesProcessor with a fake](#Replacing-messagesProcessor-with-a-fake) for more details.
2. Run the application (typically by running the `App` class in your IDE).

### Configuration

Relevant configuration for adjusting the frequency and toggle scheduler for dialog message on/off.

| Property | Description                        | Type     |
|----------|------------------------------------|----------|
| enabled  | Toggle scheduler on/off            | Boolean  |
| interval | How often scheduler sends messages | Duration |

### Replacing messagesProcessor with a fake

To run this locally (meaning without actually sending any HTTP requests) change the following in App.kt:
```kotlin
val messagesProcessor = MessagesProcessor(deps.ediAdapterClient)
```

to use `FakeEdiAdapterClient` instead:
```kotlin
val messagesProcessor = MessagesProcessor(FakeEdiAdapterClient())
```