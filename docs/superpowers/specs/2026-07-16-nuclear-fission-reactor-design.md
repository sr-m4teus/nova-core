# NovaCore — Pilar Física Nuclear, Sub-spec 3: Reator de Fissão

**Data:** 2026-07-16
**Status:** Aprovado, pronto para plano de implementação

## Contexto

Terceiro sub-spec do pilar Física Nuclear, consumidor final do `Fuel Pellet`
produzido pelo sub-spec 2 (Enriquecimento + Combustível). Fundamentado em
`docs/research/nuclear-fission-reactor.md`. Achados centrais usados
diretamente:

- Reação em cadeia é regida pelo fator de multiplicação efetivo k-eff:
  k<1 subcrítico (decai), k=1 crítico (potência estável, operação normal),
  k>1 supercrítico (cresce).
- Moderadores desaceleram nêutrons rápidos pra aumentar a seção de choque de
  fissão do U-235. Água leve é barata mas tem pior economia de nêutrons
  (por isso reatores a água leve precisam de combustível enriquecido); água
  pesada e grafite têm economia de nêutrons melhor (permitem até urânio
  natural em reatores reais), com seus próprios trade-offs de custo/risco.
- Bastões de controle (boro, cádmio, háfnio) absorvem nêutrons; inserir
  reduz k-eff (baixa potência), retirar aumenta. Inserção total ("scram")
  para a reação em cadeia rapidamente, mas **não** para o calor residual.
- PWR real: loop primário pressurizado (água, ~155 bar, >300°C sem ferver)
  troca calor com um loop secundário separado através de um trocador —
  nunca se misturam, radioatividade fica confinada ao primário.
- Eficiência térmica-elétrica real de reatores comerciais: **33-35%**
  (limite de Carnot, mesma natureza física que já limita qualquer motor
  térmico) — o resto é calor rejeitado, não convertido.
- **Calor residual (decay heat)**: produtos de fissão continuam decaindo e
  gerando calor mesmo com a reação em cadeia parada — ~7% da potência
  pré-desligamento imediatamente após, caindo a ~1% depois de uma hora,
  ~0.5% depois de um dia. Perda de refrigeração (LOCA) nessa fase ainda
  pode causar fusão do núcleo.
- Th-232 é fértil, não físsil: vira combustível (U-233) só via irradiação
  de nêutrons dentro de um reator em operação (breeding) — mecânica que
  fecha o ciclo do tório deixado em aberto nos sub-specs 1 e 2.

Todo valor de FE deste spec deriva de `EnergyScale.JOULES_PER_FE = 750`
(`src/main/java/com/novacore/energy/EnergyScale.java`), mesma constante
usada pelo Gerador Térmico e Painel Solar.

## Padrão geral: multibloco com casca + conector

Este spec introduz o primeiro conjunto de estruturas multibloco do mod —
Reator, Trocador de Calor, Turbina e um tier extra de Bateria. As quatro
compartilham o mesmo padrão arquitetural:

- **Casca**: caixa retangular de blocos `<Nome>Casing`/`<Nome>Glass`
  (glass é variante só visual, sem diferença funcional), validada por
  flood-fill disparado ao colocar/quebrar qualquer bloco de casca — se o
  volume interno fica totalmente selado, a estrutura forma. Limitado a
  **15x15x15** externo (evita scan sem limite), mínimo 5x5x5 externo
  (3x3x3 de interior útil).
- **Interior**: blocos funcionais específicos de cada estrutura (ver
  seções abaixo), colocados livremente dentro da casca — não precisa
  preencher todo o volume. Cada tipo de bloco interno escala uma
  capacidade/throughput específico, contado por número de blocos presentes.
- **Conector**: bloco `<Nome>Connector`, substitui um bloco de casca em
  qualquer posição da superfície. Clique direito cicla entre os papéis
  válidos daquela estrutura (energia/líquido/item, entrada/saída). **Só**
  blocos Connector conectam a redes externas (cabo, `LiquidNetwork`,
  hopper) — Casing/Glass puro nunca conecta a nada de fora.
- **Estado agregado**: ancorado numa posição determinística (canto de
  menor coordenada) — a estrutura inteira se comporta como um único
  provedor/consumidor pras redes que toca.
- Se a casca é rompida e o volume deixa de estar selado, a estrutura
  desforma; energia/calor acumulado fica pendente até refechar.

## Reator de Fissão

**Interior**: `ReactorFuelRod` (aceita `Fuel Pellet`, cada uma contribui
uma fatia fixa de calor/tick, ancorada no ~17 MWt/assembly real da
pesquisa, escalado), bloco de **Moderador** (Água/Água Pesada/Grafite,
colocado livremente entre varetas), e **Bastão de Controle** (bloco
colocável, inserção 0-100% configurável via GUI do reator).

Calor total do núcleo por tick:

```
calor/tick = (nº de ReactorFuelRod × calor_por_vareta)
             × multiplicador_moderador
             × (1 - inserção_bastões/100)
```

Multiplicador do moderador afeta o **rendimento total** extraído de cada
`Fuel Pellet` (não só a velocidade) — água 1.0x, água pesada 1.3x, grafite
1.15x, refletindo a economia de nêutrons real de cada material.

**`ReactorConnector`** (4 papéis): Entrada de Combustível (item, alimenta
`ReactorFuelRod`), Entrada de Tório (item, alimenta a manta fértil — ver
Breeding), Saída de Líquido Primário (bridge pra `LiquidNetwork`, carrega o
calor produzido), Saída de Urânio Bred (item, produto da manta fértil).

**Escala de saída** (âncora, com um `Fuel Pellet` = ~35.000 MJ reais de
calor de fissão):

```
35.000.000.000 J / 750 J-por-FE            = 46.666.667 FE de calor/pellet
46.666.667 FE calor × 0,34 eficiência elét. ≈ 15.866.667 FE elétricos/pellet
```

Multiplicado pelo rendimento do moderador (1.0x-1.3x) → até **~20,6M FE
elétricos** por pellet com água pesada. Taxa alvo na base (moderador água,
bastões retirados): ~2.000 FE/tick, escalando com moderador e número de
varetas — bem acima do Gerador Térmico (20 FE/tick), refletindo a
intensidade real da fissão.

## Calor residual, LOCA, meltdown

**Calor residual**: mesmo com bastões 100% inseridos ou combustível
esgotado, o núcleo segue emitindo calor por um tempo — curva decrescente
por tick a partir do calor no momento do desligamento (comprimida pra
escala de jogo: minutos, não dias), espelhando a curva real ~7%→~1%→~0.5%.

**LOCA**: sempre que o calor produzido (ativo + residual) excede o que a
rede primária consegue escoar — tubo desconectado, Trocador sem vazão
suficiente, rede quebrada — o excedente se acumula num **buffer local do
núcleo**.

**Meltdown**: buffer local acima de um limiar proporcional ao tamanho do
reator (nº de `ReactorFuelRod`, refletindo mais margem térmica em reatores
maiores) dispara o evento: blocos do núcleo destruídos, explosão de dose +
contaminação na área (reusa as trilhas de exposição do sub-spec 1), e o
terreno afetado vira **zona de radiação persistente** — exige traje hazmat
completo pra se aproximar ou limpar.

## Breeding de tório

`ReactorConnector` (Entrada de Tório) alimenta `Refined Thorium` (sub-spec
2) num buffer interno de manta fértil. Enquanto o reator está ativo (calor
ativo/tick > 0), o tório converte lentamente em `Bred Uranium`, taxa
proporcional ao calor/tick atual — reator mais ativo = mais fluxo de
nêutrons = breeding mais rápido. Zero breeding com bastões 100% inseridos.

`Bred Uranium` já é físsil (produto de irradiação, sem precisar de
centrífuga): vira uma segunda receita no **Fabricador** do sub-spec 2 —
`Bred Uranium` → `Fuel Pellet` direto, pulando a etapa de gás/enriquecimento
(fiel ao real: combustível breeder não é reenriquecido). Fecha o ciclo do
tório aberto no sub-spec 1 (minério) e adiado no sub-spec 2 (só refino).

## Trocador de Calor

**Interior**: `HeatExchangerTube`, cada um aumenta a vazão máxima de calor
transferível por tick. **`HeatExchangerConnector`** (2 papéis): Líquido
Primário / Líquido Secundário — rotulagem explícita de qual cano encostado
pertence a qual lado (substitui detecção automática ambígua).

A cada tick, move calor do buffer do lado Primário pro lado Secundário, até
o teto de vazão somado dos `HeatExchangerTube`, sem perda de transporte
(mesma convenção do cabo/tubo de gás). Primário e secundário nunca se
misturam — só trocam calor através da estrutura, radioatividade confinada
ao primário.

## Turbina

Mais detalhada que as outras três: mecânica real de estágios de rotor
extraindo energia do vapor + gerador convertendo rotação em eletricidade.

**Interior**: `TurbineRotorStage` (cada um aumenta o teto de vapor
processável por tick) e `TurbineGenerator` (cada um aumenta o teto de
conversão elétrica por tick — sem nenhum, a turbina processa vapor mas
produz 0 FE). Eficiência fixa em 34% sobre o que é efetivamente processado,
limitado pelo **menor** dos dois tetos — rotor e gerador desbalanceados
criam gargalo, mesmo problema real de dimensionamento desbalanceado.
**`TurbineConnector`** (2 papéis): Entrada de Líquido Secundário / Saída de
Energia — a saída é um `EnergyProvider` normal, plugado direto na rede de
cabo já existente do pilar Energia.

Cadeia completa: Reator (varetas) → `LiquidNetwork` primária → Trocador
(tubos) → `LiquidNetwork` secundária → Turbina (rotores+geradores) → rede
de cabo já existente → Bateria Multibloco/consumidores.

## `LiquidNetwork<K,E>`

Mesma arquitetura cacheada de `EnergyNetwork`/`GasNetwork` (grafo, merge ao
conectar, split via BFS ao desconectar). Buffer da rede guarda só
**quantidade de calor acumulado** (unidade FE-equivalente) — mais simples
que o buffer de gás do sub-spec 2 (que carregava pureza); aqui não há
"qualidade", só energia térmica. Primário e secundário do reator são duas
instâncias **separadas** dessa mesma classe genérica, nunca a mesma rede.
Tubo de água pressurizada é um bloco novo, distinto do tubo de UF6 do
sub-spec 2 (substância e rede diferentes).

## Bateria Multibloco

Tier extra de armazenamento, **acima** dos 3 tiers single-block já
existentes (Basic/Advanced/Supreme, pilar Energia — continuam intactos,
sem mudança retroativa). Motivo: o total elétrico de um `Fuel Pellet`
(~15,9M-20,6M FE) ultrapassa até a bateria Supreme (10M FE).

**Interior**: `BatteryCore` (cada um aumenta a capacidade) e
`BatteryTransfer` (cada um aumenta a taxa máxima de transferência):

```
capacidade    = CORE_UNIT × nº de BatteryCore
transferência = TRANSFER_UNIT × nº de BatteryTransfer
```

`CORE_UNIT`/`TRANSFER_UNIT` calibrados pra uma estrutura de tamanho médio
(interior ~5x5x5) cobrir confortavelmente um pellet de referência sem
precisar do teto de 15x15x15 — esse teto existe pra quem quiser escalar
além do necessário. **`BatteryConnector`** (2 papéis): Entrada de Energia /
Saída de Energia.

## Planilha de Projeto (item calculadora)

Item `Project Sheet`: clique direito abre uma GUI de calculadora pura (sem
container/inventário) — cálculo 100% cliente, já que as fórmulas acima são
funções puras sobre números que o jogador digita, sem ler o mundo.

Fluxo: jogador digita dimensões internas desejadas do Reator (a,b,c),
escolhe moderador e inserção de bastões → a planilha calcula quantas
`ReactorFuelRod` cabem (layout fixo de grade rod/moderador) → calor/tick
total → número mínimo de `HeatExchangerTube` pra escoar sem gargalo nem
sobra (dimensão y sugerida) → número mínimo de `TurbineRotorStage` +
`TurbineGenerator` casados pra processar/converter aquele calor (dimensão z
sugerida) → lista final de material (contagem de cada bloco necessário
pras três estruturas casadas). Reaproveita o mesmo espírito de "escala
derivada de fórmula documentada" já usado em todo o mod — a planilha não
inventa números novos, só aplica as fórmulas deste spec de trás pra frente.

## Testes

**Unitários puros** (mesmo padrão de `EnergyNetworkTest`/sub-spec 2):
`LiquidNetwork` (calor flui de mais pra menos, respeita teto de vazão, sem
perda, merge/split preserva total); fórmula de calor do reator (varetas ×
moderador × bastões); curva de calor residual; limiar de meltdown; taxa de
breeding proporcional ao calor ativo; fórmulas da Planilha de Projeto
batendo contra as fórmulas reais dos blocos.

**GameTest**: formação de casca válida/inválida nas 4 estruturas; ciclo
completo Reator→Trocador→Turbina→cabo→Bateria Multibloco acumulando FE;
LOCA (rede primária desconectada) disparando meltdown e contaminação num
jogador próximo sem hazmat; breeding de tório produzindo `Bred Uranium`
com reator ativo e nada com bastões 100% inseridos.

## Escopo explícito

**Neste spec**: 4 estruturas multibloco (Reator, Trocador de Calor,
Turbina, Bateria) com padrão casca+conector+blocos internos escaláveis;
`LiquidNetwork`; moderador como multiplicador de rendimento; bastões de
controle (0-100%, k-eff); calor residual pós-desligamento; LOCA e meltdown
com contaminação persistente de área; breeding de tório em manta fértil
(`Bred Uranium`, nova receita no Fabricador do sub-spec 2); item Planilha
de Projeto; novo tier de bateria `Massive` (multibloco).

**Fora de escopo** (sub-specs futuros): lixo nuclear / combustível gasto
pós-uso; modos de falha avançados do moderador (Efeito Wigner do grafite,
flamabilidade); automação/integração com redstone da GUI dos multiblocos;
material grau-arma; texturas reais (mantém convenção Blockbench + textura
placeholder de cor sólida já estabelecida nos sub-specs anteriores).
