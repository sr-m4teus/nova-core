# NovaCore — Pilar Energia: Design

**Data:** 2026-07-14
**Status:** Aprovado, pronto pra plano de implementação

## Contexto

NovaCore é um mod técnico de Minecraft cujo tema central é Tecnologia, Energia e Física
(clássica, nuclear, quântica). Nada no mod deve extrapolar a realidade além dos limites
impostos pelo próprio Minecraft — mecânicas devem se aproximar ao máximo de conceitos e
valores físicos reais.

O mod será construído por partes, sem pressão de prazo ou MVP. Este é o primeiro
sub-projeto (dos três pilares: Energia, Física Nuclear, Física Quântica), escolhido como
ponto de partida porque os outros dois pilares dependem da infraestrutura de energia já
existir.

Todo bloco/item novo deve ter um arquivo Blockbench correspondente definindo seu design
visual. Toda mecânica nova deve ser fundamentada em pesquisa sobre o fenômeno físico real
que ela representa, antes de definir valores de jogo.

## Fundação técnica

- **Mod id:** `novacore`
- **Package raiz:** `com.novacore.*`
- **Loader:** NeoForge para Minecraft **26.1**
- **Toolchain:** Java 25, Gradle 9.1+
- **Registro:** `DeferredRegister` padrão NeoForge para items, blocks e block entities
- **API de energia:** `EnergyHandler` (API atual do NeoForge 26.1, sucessora de
  `IEnergyStorage`/estilo Forge Energy — trocada na "Transfer Rework" da 21.9). O mod
  adota essa API como padrão de compatibilidade com o ecosystem de mods técnicos.
- **Pipeline de assets:** todo bloco/item novo tem um arquivo `.bbmodel` correspondente
  na pasta `blockbench/` na raiz do repositório, versionado junto do código-fonte, e
  exportado para `src/main/resources/assets/novacore/...`

## Rede de energia — grafo cacheado

Decisão: investir desde já numa estrutura de rede em grafo cacheado (não um push simples
vizinho-a-vizinho), para evitar retrabalho de arquitetura quando a escala de bases dos
jogadores crescer.

- Cada cluster contíguo de cabos forma um objeto `EnergyNetwork` — grafo onde nós
  representam cabos e arestas representam conexões físicas adjacentes.
- **Invalidação incremental:** ao colocar um cabo adjacente a uma rede existente, o nó é
  inserido na rede (merge se conectar duas redes distintas). Ao quebrar um cabo, a rede
  faz split em sub-grafos independentes se a remoção desconectar o cluster.
- Cada `EnergyNetwork` mantém referências para os providers (geradores, baterias
  descarregando) e consumers (máquinas, baterias carregando) conectados nas suas bordas,
  via capability `EnergyHandler`.
- **Distribuição por tick:** um único pass por rede. Soma a demanda de todos os
  consumers conectados, distribui a energia disponível dos providers proporcionalmente
  à demanda. Sem perda de energia por transporte (decisão explícita: cabos não têm
  resistência/perda).
- Cabos: um único tier por enquanto, throughput flat (sem conceito de voltagem/tensão).
  Ligar qualquer máquina a qualquer cabo não causa dano ou explosão — throughput é o
  único limite.
- Testado isoladamente da lógica de mundo do Minecraft (ver seção Testes).

## Geração

### Gerador térmico (combustão)
- Queima combustível sólido/líquido (carvão inicialmente; óleo como extensão futura),
  convertendo energia térmica em elétrica via um modelo simplificado de ciclo
  termodinâmico.
- Valores de energia gerados por unidade de combustível devem ser pesquisados a partir
  da densidade energética real do material (ex: MJ/kg do carvão) e então escalados para
  a economia do jogo — não inventados livremente.
- Possui buffer interno pequeno; empurra energia para a rede conectada via
  `EnergyHandler`.

### Painel solar (fotovoltaico)
- Geração proporcional à luz solar recebida: posição do sol no céu, bioma, obstrução por
  blocos acima do painel.
- Curva de output baseada em irradiância solar real (pico ~1000 W/m² em condições
  ideais), escalada para o jogo.
- Geração cai a zero à noite; reduzida sob chuva/tempo nublado (reflete comportamento
  real de painéis fotovoltaicos).

## Armazenamento — bateria, 3 tiers

- Três blocos distintos: Básica / Avançada / Suprema.
- Capacidade escalada entre tiers (proporção exata definida durante a pesquisa/balance
  de cada tier, referência inicial: ~10x por tier).
- Cada bateria atua como nó terminal do grafo: consumer quando carregando, provider
  quando descarregando, ambos via `EnergyHandler`.
- Bloco físico no mundo (não item portátil nesta fase).

## Máquina consumidora — Fornalha Elétrica

- Substitui combustível sólido por energia elétrica para smelting, provando o ciclo
  completo fim-a-fim: gerador → cabo → rede → fornalha.
- Consumo de energia por operação de smelt baseado em aproximação real de energia
  necessária para fusão/aquecimento de materiais comuns, escalado para o jogo.
- Sem consumo de fuel. Sem risco de dano por overload (consequência da ausência de tiers
  de voltagem).

## Tratamento de casos de borda

- **Storage cheio:** geração excedente é descartada sem efeito colateral (sem explosão
  por overflow — decisão explícita).
- **Rede sem consumers:** providers não geram energia desnecessariamente; tick é
  pulado quando a demanda total da rede é zero.
- **Cabo quebrado no meio de uma rede:** o grafo faz split automático em dois (ou mais)
  sub-grafos independentes.
- **Loop de cabos:** tratado como grafo comum — cada nó é visitado uma vez por pass,
  sem duplicar energia distribuída.

## Testes

- **NeoForge GameTest framework** para o ciclo automatizado completo em mundo de teste:
  gerador → cabo → bateria → fornalha elétrica smelta um item.
- **Testes unitários puros** para a lógica do grafo (merge, split, distribuição
  proporcional), sem dependência do mundo do Minecraft — permite testar a rede como
  estrutura de dados isolada.

## Escopo explícito

Este spec cobre: geração (térmica + solar), armazenamento (3 tiers), transporte (cabo
único tier, grafo cacheado), API de energia, e uma máquina consumidora mínima (fornalha
elétrica) para provar o ciclo fim-a-fim.

**Fora de escopo** (specs futuros): múltiplas máquinas consumidoras adicionais, tiers de
voltagem/proteção contra sobrecarga, tiers de cabo com perda, itens de bateria portátil,
integração com os pilares de Física Nuclear e Física Quântica.
