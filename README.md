# UMH-30 Editor v0.6-alpha

## Alteração principal

Ao conectar o UMH-30, o aplicativo abre automaticamente:

- INPUT 1..3: app -> UMH-30
- OUTPUT 1..3: UMH-30 -> app

Isso corrige o erro `nenhuma input port está aberta` observado ao enviar routing.

As três OUTPUT continuam sendo monitoradas para mensagens de identificação dos USB Host.

## USB Host names

O parser reconhece:

F0 00 21 5E 02 06 01 SLOT LENGTH ASCII... F7

Exemplos confirmados:
- USB 1 = W-FADER
- USB 2 = SINCO
- USB 3 = FM-1

A v0.6 não inventa um comando de consulta para os nomes. Ela apenas escuta as mensagens reais do UMH-30. O request usado pelo MIDI Stream para provocar essas respostas será integrado depois de identificado a partir da captura.

## Routing

A interface continua com MIDI IN 1/2 + USB Host 1..8 à esquerda e MIDI OUT 1/2 + USB Host 1..8 à direita.

As conexões da mesma porta são bloqueadas.

`ENVIAR AO UMH-30` envia as rotas selecionadas e depois o SET/SAVE.
