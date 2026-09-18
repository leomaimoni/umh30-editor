# UMH-30 Editor v0.7-alpha

- Opens all available UMH-30 MIDI input and output ports concurrently.
- On connection, sends the routing read request observed in the DOREMiDi capture:
  `F0 00 21 5E 02 01 00 00 00 00 7F F7`
- Also sends the observed USB-host enumeration request:
  `F0 00 21 5E 01 03 00 00 00 00 7F F7`
- Parses incoming `02 01` routing frames with state `01` and rebuilds active connections.
- Parses USB-host name frames `02 06 01 SLOT LENGTH ASCII... F7`.
- Adds a manual `LER CONFIGURAÇÃO DO UMH-30` button.
- Existing routing UI and SET/SAVE remain available.

The read request and route-response parsing are based on the supplied PCAPNG captures. The app logs every received SysEx frame so the next test can confirm which port carries the UMH-30 state response.
