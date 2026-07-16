# NovaCore — Pilar Física Nuclear, Sub-spec 2: Enriquecimento + Combustível

**Data:** 2026-07-15
**Status:** Aprovado, pronto para plano de implementação

## Contexto

Este é o segundo sub-spec do pilar Física Nuclear, ponte entre o sub-spec 1
(Radiação + Minérios, já implementado: jazidas de urânio/tório, dose,
blindagem, contaminação, contador Geiger, scanner de prospecção) e o futuro
sub-spec 3 (Reator de Fissão, que consumirá o `Fuel Pellet` produzido aqui).

Fundamentado em `docs/research/nuclear-enrichment-fuel-cycle.md`. Achados
centrais usados diretamente:

- Cadeia real: mineração → moagem (yellowcake, U₃O₈) → conversão (UF₆, gás a
  partir de ~56°C) → enriquecimento (centrífugas em cascata) → fabricação de
  combustível (pellets sinterizados de UO₂).
- Urânio natural é ~0.7% U-235; combustível civil (LEU) fica na faixa 3-5%;
  HEU (risco de proliferação) começa em 20% — subir de 0.7% para 20% já
  consome ~90% do esforço total de separação até chegar a 90% (grau-arma).
  Esse não-linearidade é o motivo para o mod nunca se aproximar da linha HEU.
- Cada centrífuga individual produz um ganho de enriquecimento pequeno;
  centrífugas reais são encadeadas fisicamente em cascatas de centenas de
  estágios, com o produto de uma alimentando a próxima.
- Custo energético real de enriquecimento por centrífuga: ~50 kWh/SWU
  (ordens de magnitude mais barato que difusão gasosa, método antigo
  retirado de uso, ~2.400-2.500 kWh/SWU — fora de escopo deste spec).
- Tório (Th-232) é **fértil, não físsil**: não enriquece por centrífuga na
  realidade. Só se torna combustível via irradiação dentro de um reator
  (breeding, Th-232 + nêutron → U-233) — mecânica que pertence ao sub-spec do
  reator, não a este.
- Combustível de urânio enriquecido é ordens de magnitude mais denso
  energeticamente que carvão (âncora de `EnergyScale`) — a conversão exata
  para FE fica para o sub-spec do reator, que é onde essa energia é
  efetivamente liberada; este spec só cobre o *processamento* até o pellet.

## Cadeia de processamento

Três blocos novos, cadeia física em série:

1. **Refinaria**: `Raw Uranium` → `Yellowcake` (U₃O₈). `Raw Thorium` →
   `Refined Thorium`. GUI container simples (1 slot de entrada, 1 de saída,
   barra de progresso, consumo de FE/tick), mesmo padrão de
   `ElectricFurnaceMenu`/`ElectricFurnaceScreen` já usado no pilar Energia.
   Simplifica mineração+moagem+conversão reais num único passo de jogo — o
   mod não precisa de um item separado para "minério concentrado
   pré-yellowcake", já que nenhuma mecânica futura depende dessa distinção.

2. **Centrífuga** (só urânio — tório não passa por aqui, ver seção
   "Tório"): sem slots de item. Um hopper/inventário adjacente alimenta
   `Yellowcake` na primeira centrífuga da rede de gás, que o converte
   internamente para UF₆ gasoso (colapsando a etapa real de "conversão"
   dentro do intake da centrífuga, já que não há uso de jogo para um bloco
   "Conversor" dedicado) e o injeta na rede já com o primeiro incremento de
   enriquecimento. Sem GUI container; clique direito mostra leitura em tempo
   real via action bar (pureza atual e volume do buffer da rede conectada),
   mesmo estilo do contador Geiger — evita duplicar a infraestrutura de rede
   cliente-servidor já construída para o scanner do sub-spec 1.

3. **Fabricador de combustível**: puxa UF₆ enriquecido direto do buffer da
   rede de gás (via face conectada ao tubo, sem slot de entrada de item), 1
   slot de saída (`Fuel Pellet`), barra de progresso, consumo de FE/tick.

## Fluido UF6 e rede de tubo pressurizado

`UF6Gas`: fluido novo, sistema próprio (não usa a API de fluido genérica do
NeoForge) porque UF₆ carrega um dado que fluido comum não modela: **pureza**
(%U-235), além de quantidade.

**GasNetwork\<K,E\>**: mesma arquitetura do `EnergyNetwork`/
`EnergyNetworkManager` do pilar Energia — grafo cacheado, merge ao conectar,
split via BFS ao desconectar. Diferença chave em relação à rede de energia:
a rede de gás guarda um **buffer compartilhado com estado** (quantidade em
mB + %U-235 média ponderada por volume), não é só um mecanismo de
distribuição por tick — reflete que o gás precisa acumular e ganhar pureza
progressivamente ao atravessar centrífugas, diferente de FE que é
instantâneo.

Comportamento por tick:

- A Centrífuga injeta UF₆ fresco (~0.7% natural) no buffer da rede ao
  processar `Yellowcake`.
- Cada Centrífuga conectada à mesma rede aplica um incremento de pureza ao
  buffer, proporcional ao número de centrífugas ativas naquela rede — mais
  centrífugas encadeadas fisicamente (via tubo) = cascata mais rápida/mais
  alta, refletindo diretamente a pesquisa. O incremento converge
  assintoticamente para um **teto de ~5-8% (faixa LEU)**, nunca alcançável
  além disso, e a curva de custo/ganho por ponto percentual é não-linear
  (cada ponto perto do teto custa mais FE que o anterior), espelhando o real
  ~90% do esforço gasto entre 0.7% e 20%.
- Merge de duas redes de gás com pureza diferente: nova pureza = média
  ponderada por volume dos dois buffers.
- O Fabricador extrai do buffer (quantidade + pureza no momento da
  extração) → `Fuel Pellet` com aquela pureza gravada.
- Custo energético de cada Centrífuga ativa: FE/tick derivado do real ~50
  kWh/SWU via `EnergyScale`, escalado pela curva não-linear acima.

**Tubo pressurizado**: bloco novo, transporta exclusivamente `UF6Gas` (rede
separada da rede de cabos de energia). Quebrar o tubo com gás dentro libera
o conteúdo na área ao redor, disparando a trilha de contaminação já
existente do sub-spec 1 (mesmo acumulador; sem traje hazmat completo =
contaminado) — reforça o risco real de manusear gás radioativo pressurizado
e reusa mecânica já validada em vez de criar uma nova.

## Tório — tratamento à parte

Th-232 é fértil, não físsil: fisicamente não enriquece por centrífuga. Neste
spec, tório passa **só** pela Refinaria (`Raw Thorium` → `Refined Thorium`)
e para por aí — vira matéria-prima parada, sem cadeia de gás, sem
Centrífuga, sem Fabricador. Breeding (Th-232 + nêutron → U-233 dentro de um
reator em operação) é mecânica do sub-spec 3 (Reator de Fissão), que ainda
não existe. Modelar tório seguindo a mesma cadeia do urânio seria fisicamente
incorreto e violaria a lei de design do mod.

## Testes

- **Unitários puros** (mesmo padrão de `EnergyNetworkTest`): merge de
  pureza por média ponderada de volume ao unir duas redes; curva de
  convergência não-linear do incremento de pureza por tick em direção ao
  teto; cálculo de custo FE por estágio a partir do valor real de SWU.
- **GameTest**: cadeia completa — Refinaria processa `Raw Uranium` até
  `Yellowcake`; Centrífuga conectada por tubo converte e enriquece;
  Fabricador extrai e produz `Fuel Pellet` com pureza > 0. Teste separado:
  quebrar o tubo pressurizado com gás dentro dispara contaminação em um
  jogador próximo sem traje hazmat.

## Escopo explícito

**Neste spec**: Refinaria, Centrífuga (cascata via rede de gás
compartilhada), Fabricador de combustível; fluido `UF6Gas` e tubo
pressurizado com `GasNetwork` cacheada própria; itens `Yellowcake`,
`Refined Thorium`, `Fuel Pellet`; teto de enriquecimento em faixa LEU
(~5-8%); ruptura de tubo pressurizado disparando contaminação.

**Fora de escopo** (sub-specs futuros): Reator de Fissão (consumidor do
`Fuel Pellet` e ponto onde a energia real do combustível é convertida para
FE via `EnergyScale`); breeding de tório (Th-232 → U-233); difusão gasosa
como método alternativo de enriquecimento; material grau-arma ou qualquer
mecânica acima da faixa LEU; sistema de fluido genérico reutilizável por
outras mecânicas do mod (o tubo pressurizado é propositalmente específico
para UF₆, não uma API de fluido geral). Texturas reais também ficam fora de
escopo — blocos e itens novos seguem a mesma convenção Blockbench + textura
placeholder de cor sólida já estabelecida nos pilares anteriores.
