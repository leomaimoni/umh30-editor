# UMH-30 Editor v0.5-alpha

Editor visual de routing com tentativa de identificação dos dispositivos USB Host.

## Identificação USB Host

O app escuta as 3 portas OUTPUT MIDI expostas pelo UMH-30 ao Android.

Quando recebe o padrão:

F0 00 21 5E 02 06 01 SLOT LENGTH ASCII... F7

ele decodifica o nome e atualiza a interface.

Exemplos confirmados nas capturas:

USB 1 = W-FADER
USB 2 = SINCO
USB 3 = FM-1

## Routing

Esquerda:
- MIDI IN 1
- MIDI IN 2
- MIDI USB 1..8

Direita:
- MIDI OUT 1
- MIDI OUT 2
- MIDI USB 1..8

A mesma porta não pode ser conectada a ela própria.

ENVIAR AO UMH-30 envia as mensagens de routing e depois o SET/SAVE.

## Nota

A identificação é feita a partir das mensagens que o UMH-30 efetivamente entrega pelo MIDI. Se o firmware só emitir essas mensagens em determinada ação, o app mostrará os nomes assim que recebê-las.
