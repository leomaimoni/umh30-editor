# UMH-30 Editor

Editor Android experimental para o DOREMiDi UMH-30.

## Objetivo da primeira alpha

A primeira versão valida a infraestrutura antes de implementar todo o protocolo:

- detectar o UMH-30 via USB-MIDI;
- abrir as portas MIDI;
- receber dados MIDI/SysEx;
- enviar SysEx;
- mostrar o tráfego em hexadecimal;
- enviar o comando SET/SAVE que foi confirmado na captura USBPcap.

## O que ainda NÃO está implementado

- leitura da matriz de routing;
- escrita individual de uma rota;
- nomes dos USB Host;
- MIDI Filter;
- MIDI Mapping.

Essas partes serão adicionadas somente depois de confirmarmos os bytes exatos do protocolo nas capturas.

## Build

O GitHub Actions compila automaticamente o APK de debug a cada push em `main`/`master`, e também pode ser executado manualmente.

## Estrutura

`MidiUsbHelper.kt`
- transporte Android MIDI.

`Umh30Protocol.kt`
- comandos SysEx específicos do UMH-30.

`MainActivity.kt`
- interface.

`.github/workflows/build.yml`
- build automático do APK.

## Próximo teste

1. Conectar o UMH-30 ao Android.
2. Abrir o app.
3. Verificar se o dispositivo aparece.
4. Connect.
5. Observar Inputs/Outputs.
6. Pressionar SEND SAVE / SET somente quando o aparelho estiver conectado.
7. Enviar o log RX/TX para a próxima etapa.

A versão não altera routing automaticamente.
