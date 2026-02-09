# helsemelding-epj-simulator

Application for simulating behaviour of an EPJ (Elektronisk pasientjournal) system through EDI 2.0. 
The counterpart in this flow is [helsemelding-message-generator](https://github.com/navikt/helsemelding-message-generator) 
which is producing the messages this application will be consuming.

Overall functionality:
- Poll for new messages sent
- Send `apprec` for message
- Update message as `read`