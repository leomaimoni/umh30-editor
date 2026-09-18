# UMH-30 Editor v0.4-alpha

Primeira versão do editor visual de routing.

## Interface

Duas colunas:
- esquerda: MIDI IN 1, MIDI IN 2 e MIDI USB 1..8;
- direita: MIDI OUT 1, MIDI OUT 2 e MIDI USB 1..8.

Toque em uma entrada e depois em uma saída. Uma linha representa a ligação.

A própria interface bloqueia:
- MIDI IN 1 -> MIDI OUT 1;
- MIDI IN 2 -> MIDI OUT 2;
- MIDI USB N -> MIDI USB N.

## Envio

Ao pressionar ENVIAR AO UMH-30:
1. envia um SysEx de routing para cada ligação;
2. envia o SET/SAVE confirmado;
3. registra os bytes em hexadecimal no log.

## Protocolo

A codificação de routing usada nesta versão é a que foi recuperada durante a investigação do UMH-30:
F0 00 21 5E 02 01 SRC_TYPE SRC_INDEX DST_TYPE DST_INDEX STATE F7

00 = MIDI físico
01 = USB Host
01 = connect
00 = disconnect

SET/SAVE:
F0 00 21 5E 02 02 00 F7

## Observação

Esta versão fixa 8 slots USB Host, que é o limite informado para o hub do UMH-30. Os nomes reais dos dispositivos USB ainda dependem da forma como o UMH-30 expõe esses dispositivos ao Android.
