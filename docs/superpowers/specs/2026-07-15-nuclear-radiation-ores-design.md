# NovaCore — Pilar Física Nuclear, Sub-spec 1: Radiação + Minérios

**Data:** 2026-07-15
**Status:** Aprovado, pronto para plano de implementação

## Contexto

Este é o primeiro sub-spec do pilar Física Nuclear (o segundo dos três pilares
centrais do NovaCore: Energia → Física Nuclear → Física Quântica). O pilar de
Energia está completo e serve de referência de padrão: valores de jogo
derivados de física real através de uma constante de escala documentada
(`EnergyScale`), pesquisa registrada antes de cada mecânica, e testes
unitários puros para a lógica isolada da integração com o Minecraft.

Física Nuclear é grande demais para um spec único, então foi quebrada em
sub-specs (enriquecimento, reator de fissão, lixo nuclear ficam para depois).
Este primeiro sub-spec é a fundação: sem minério radioativo, exposição do
jogador e forma de detectar/proteger-se, nenhuma mecânica nuclear posterior
tem contexto de risco real para se apoiar.

Seis documentos de pesquisa fundamentam este spec, em `docs/research/`:
`nuclear-radioactive-ores.md`, `nuclear-radioactive-decay.md`,
`nuclear-radiation-dosimetry.md`, `nuclear-enrichment-fuel-cycle.md`,
`nuclear-fission-reactor.md`, `nuclear-waste-handling.md`. Achados centrais
usados diretamente neste spec:

- Urânio não é raro atomicamente (~2.7 ppm, comparável ao estanho); minério
  econômico exige concentração geológica de ordem ~400x via processos
  geológicos específicos — não é distribuído uniformemente pelo mundo.
- Tório é 3-4x mais abundante que urânio na crosta terrestre.
- Minério bruto (pitchblenda) é apenas levemente radioativo ao contato
  (dezenas de µSv/h) — a radiação de minério cru não é, na realidade, um
  perigo agudo. O perigo real de mineração é a inalação de poeira/partículas,
  não a dose gama do minério em si.
- Meia-vida real de U-238/U-232(tório) é da ordem de bilhões de anos —
  irrelevante para decaimento observável numa sessão de jogo. Decaimento só
  importa como mecânica quando isótopos de meia-vida curta entram em cena
  (combustível processado, produtos de fissão, lixo nuclear) — fora de
  escopo aqui.
- Trajes hazmat reais **não bloqueiam radiação gama** — eles protegem contra
  contaminação por poeira/partículas radioativas (inalação, contato). A
  blindagem real contra gama vem de massa densa (chumbo, concreto), pela
  lógica da camada semi-redutora (cada espessura fixa de chumbo reduz a
  intensidade pela metade).
- Limiares reais de síndrome aguda de radiação (ARS): sem efeito clínico
  abaixo de ~0.1 Sv; sintomas leves surgem perto de 0.5-1 Sv; a dose
  letal mediana (LD50) sem tratamento fica na faixa de 4-5 Sv.
- Prospecção real de urânio usa levantamento radiométrico (aéreo, de solo, ou
  logging de furos de sondagem) para localizar depósitos antes de escavar —
  não detecção gama passiva de longo alcance através de rocha sólida (que na
  realidade tem alcance de poucos centímetros).

## Minérios — Jazidas

Urânio e Tório geram como **jazidas**: depósitos concentrados (formação tipo
blob/elipsoide, ~20-40 blocos por jazida), esparsos pelo mundo — bem mais
raros que um veio de minério vanilla comum. Isso reflete diretamente a
pesquisa: minério econômico não é distribuído uniformemente, exige
concentração geológica localizada. Tório gera com frequência
proporcionalmente maior que urânio (~3-4x), batendo com sua abundância real
relativa.

Cada jazida contém blocos de minério (`Uranium Ore` / `Thorium Ore`, variantes
stone e deepslate, seguindo a convenção vanilla de minérios) que dropam os
itens brutos correspondentes (`Raw Uranium`, `Raw Thorium`), mesma convenção
de `raw_iron`/`raw_copper`.

Uma jazida vaza radiação suficiente para ser detectável a distância, mesmo
enterrada (atenuada pela rocha ao redor, mas não bloqueada por completo) —
habilita a mecânica de prospecção da seção seguinte.

## Sistema de dose de radiação do jogador

**Cálculo**: scan direto por raio (~12 blocos ao redor do jogador),
executado a cada N ticks (não todo tick, já que dose é uma estatística de
acúmulo lento, não precisa de precisão de 20Hz) — não um campo pré-computado
tipo motor de luz, que seria complexidade desnecessária para a quantidade de
minério que existe nesta fase do mod.

Para cada bloco radioativo dentro do raio, a contribuição de dose-rate segue
a **lei do inverso do quadrado**: `dose_rate = fonte_constante / distância²`,
com distância mínima de 1 bloco para evitar singularidade. Item bruto
carregado no inventário do jogador conta como uma fonte adicional a uma
distância fixa curta (carregar minério bruto no bolso pesa proporcionalmente
mais que o mesmo minério a alguns blocos de distância, refletindo a intuição
real de "não guarde urânio no bolso").

Os valores de dose-rate por bloco são derivados da pesquisa (dezenas de µSv/h
ao contato com pitchblenda real) — **minério bruto é deliberadamente quase
inofensivo em dose gama pura**, consistente com a pesquisa. O contador Geiger
(próxima seção) existe para o jogador *perceber* que há radiação presente,
não porque o minério cru represente perigo agudo por si só nesta fase do
pilar.

**Acúmulo e efeitos**: o jogador acumula uma dose persistente (Sv, valor
double), que decai lentamente quando o jogador não está exposto (reflete a
recuperação biológica real de curto prazo para doses sub-letais). Efeitos
aplicados em estágios, ancorados nos limiares reais de ARS pesquisados:

- Abaixo de ~0.1 Sv acumulado: nenhum efeito.
- ~0.1-1 Sv: náusea leve.
- ~1-2 Sv: fraqueza + náusea mais forte.
- ~2-4 Sv: efeitos fortes (fome/veneno).
- Próximo de 4-5 Sv: risco de morte por dano contínuo, ancorado na faixa
  real de LD50 sem tratamento.

## Blindagem e contaminação — duas trilhas separadas

A pesquisa identificou uma nuance real importante: **traje hazmat não
bloqueia radiação gama**; ele protege contra contaminação por poeira e
partículas radioativas. Blindagem real contra gama vem de massa densa. O
NovaCore modela isso como duas trilhas de exposição independentes, cada uma
com sua própria contramedida:

**Trilha 1 — Dose gama**: mitigada por **blocos de chumbo**. Cada bloco de
chumbo sólido no caminho de visada entre uma fonte radioativa e o jogador
atenua a contribuição daquela fonte específica pela metade (simplificação do
conceito real de camada semi-redutora). O traje hazmat **não** afeta esta
trilha.

**Trilha 2 — Contaminação**: acumulador separado, próprio, com seu próprio
decaimento ao longo do tempo e seus próprios efeitos (náusea/veneno).
Disparada quando o jogador quebra um bloco de minério radioativo **sem** o
traje hazmat completo (4 peças, seguindo a convenção vanilla de slots de
armadura) equipado — exige o conjunto completo, não peças parciais, refletindo
a prática real de trajes de corpo inteiro. Blocos de chumbo não afetam esta
trilha; só o traje hazmat protege contra ela.

## Ferramentas de detecção

**Contador Geiger**: item de leitura passiva/ativa. Mostra a taxa de dose
atual na posição do jogador em tempo real, reaproveitando o mesmo cálculo do
sistema de dose (consulta a taxa instantânea, não o acumulado).

**Scanner de prospecção**: item de uso ativo. Ao clicar com o botão direito,
abre uma interface. O servidor varre um raio de ~5 chunks ao redor do
jogador em busca de jazidas (mesmo através de rocha sólida, já que jazidas
"vazam" o suficiente para ativar o scanner), envia o resultado para o
cliente via um pacote de rede dedicado, e o cliente renderiza um **radar
visual 2D top-down**: jogador no centro, jazidas detectadas marcadas como
pontos na grade.

Fundamentação real: o alcance de ~5 chunks e a detecção através de rocha
sólida não representam detecção gama passiva literal (que na realidade tem
alcance de poucos centímetros através de rocha) — representam a tecnologia
real de levantamento radiométrico/logging de sondagem que empresas de
mineração usam para mapear depósitos antes de escavar. A *existência* e o
*propósito* da ferramenta são fundamentados na realidade; o alcance exato é
uma escolha de jogabilidade sobre um fluxo de prospecção real (múltiplos
pontos de sondagem combinados em um mapa), no mesmo espírito das escalas de
jogabilidade já usadas no pilar de Energia.

Esta é a primeira mecânica do mod que exige uma tela informativa alimentada
por uma resposta de rede explícita (payload cliente-servidor), diferente do
padrão de menu de contêiner com sincronização automática usado nas GUIs do
pilar de Energia — infraestrutura nova a ser construída.

## Testes

- **Testes unitários puros**: função de cálculo de dose-rate por inverso do
  quadrado, cálculo do multiplicador de atenuação por blocos de chumbo no
  caminho de visada, e o mapeamento de limiar-de-dose-acumulada para estágio
  de efeito — extraídos como lógica pura testável sem depender de mundo do
  Minecraft, mesmo padrão usado em `EnergyNetwork`.
- **GameTest**: jogador posicionado perto de um bloco radioativo acumula
  dose ao longo dos ticks; um bloco de chumbo interposto reduz a taxa de
  acúmulo; quebrar minério sem traje hazmat completo dispara contaminação;
  quebrar com traje completo não dispara.

## Escopo explícito

**Neste spec**: minério de urânio e tório gerando em jazidas concentradas;
sistema de dose de radiação do jogador (bloco + item, inverso do quadrado,
efeitos em estágio por limiar real); blindagem de chumbo; contaminação e
traje hazmat como trilha separada; contador Geiger; scanner de prospecção
com radar visual.

**Fora de escopo** (sub-specs futuros do pilar Física Nuclear): enriquecimento
de urânio, fabricação de combustível, reator de fissão, lixo nuclear e
decaimento simulado ao longo do tempo (só relevante quando isótopos de
meia-vida curta existirem no mod). Texturas reais também ficam fora de
escopo — blocos e itens novos seguem a mesma convenção Blockbench +
textura placeholder de cor sólida já estabelecida no pilar de Energia.
