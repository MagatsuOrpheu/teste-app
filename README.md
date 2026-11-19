# Meu Financeiro

App Android offline para controle financeiro mensal. Você informa quanto recebe e seus gastos fixos, e o app mostra quanto ainda está **livre para gastar** no mês e quanto isso dá por dia.

## Como instalar no celular
O GitHub Actions compila o app a cada envio para a branch `main`. Abra **Releases** neste repositório, baixe o `MeuFinanceiro.apk` mais recente e instale (permita "instalar apps desconhecidos" quando o Android pedir).

## O que ele faz
- **Primeira abertura:** pergunta a renda mensal líquida, o dia do pagamento e os gastos fixos.
- **Início:** saldo livre, quanto dá por dia, % da renda comprometida, contas pagas e pendentes, alertas.
- **Gastos:** lançamentos do dia a dia e rendas extras.
- **Fixos:** rendas e gastos fixos que se repetem todo mês, com marcação de "pago" e contas atrasadas em vermelho.
- **Resumo:** gastos por categoria e renda x gastos dos últimos 6 meses.
- **Ajustes:** categorias com cor e limite mensal opcional.

## Stack
Kotlin 2.0 · Jetpack Compose · Material 3 · Room · MVVM com StateFlow · minSdk 26 (Android 8).
