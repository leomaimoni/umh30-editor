# UMH-30 Editor v0.2-alpha

Versão de diagnóstico das portas MIDI Android.

Objetivo: descobrir individualmente quais portas INPUT e OUTPUT do UMH-30 podem ser abertas pelo Android.

O app:
- detecta o UMH-30;
- abre o dispositivo sem abrir portas automaticamente;
- mostra as 3 INPUT e 3 OUTPUT;
- testa cada INPUT individualmente;
- testa cada OUTPUT individualmente;
- registra sucesso/erro;
- permite enviar apenas o comando SAVE/SET já confirmado nas capturas.

Ainda não implementa routing, USB Host names, Filter ou Mapping.

## Teste

Depois de instalar:
1. conecte o UMH-30;
2. abra o app;
3. pressione OPEN DEVICE;
4. teste INPUT 0, 1 e 2;
5. teste OUTPUT 0, 1 e 2;
6. não pressione SAVE ainda;
7. copie/mande o DIAGNOSTIC LOG.
