# Voice Bubble text-sharing contract

Voice Bubble remains a standalone application. It does not bind to, query, or require
the separate Clipboard application.

## User-initiated send

After a transcript is produced, a user can long-press the floating bubble and choose
**Send / Share**. Voice Bubble then launches an Android chooser with:

* action: `Intent.ACTION_SEND`
* MIME type: `text/plain`
* transcript: `Intent.EXTRA_TEXT`

Any installed Clipboard application can register a standard `ACTION_SEND` text
receiver and will appear in that chooser. If it is not installed, the user can choose
another compatible app; Voice Bubble's Copy action and local Room history remain
available. Voice Bubble verifies that the intent has a compatible handler before
opening the chooser. No transcript is sent without the user's tap.
