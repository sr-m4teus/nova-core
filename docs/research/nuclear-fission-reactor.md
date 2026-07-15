# Nuclear Fission Reactor — Research Notes

Pure research to ground the design of a future "nuclear fission reactor" multiblock in
NovaCore's Nuclear Physics pillar. No implementation decisions here — just the real-world
physics/engineering facts the design must not contradict, plus non-binding candidate ideas
for how the mod could represent them. All FE figures should ultimately derive from
`EnergyScale.JOULES_PER_FE = 750` (see `src/main/java/com/novacore/energy/EnergyScale.java`),
the same way the Thermal Generator (20 FE/tick from coal's ~24 MJ/kg) and Solar Panel
(2.667 FE/tick peak from a ~200 m² panel at 20% efficiency) already do.

## 1. Core fission physics

**Chain reaction.** A fissile nucleus (e.g. U-235) absorbs a neutron, splits into two lighter
fission-product nuclei, and releases 2-3 new neutrons plus ~200 MeV of energy. If enough of
those new neutrons go on to cause further fissions, the process is self-sustaining — a chain
reaction.

**Criticality** is measured by the effective neutron multiplication factor, k-eff — the ratio
of neutrons produced in one generation to neutrons lost (via absorption or leakage) in the
previous generation:
- **k < 1 — subcritical**: the neutron population (and power) decays over time; the reaction
  is not self-sustaining.
- **k = 1 — critical**: the neutron population is constant generation to generation; steady
  self-sustaining power output. This is the normal operating state of a power reactor.
- **k > 1 — supercritical**: the neutron population grows exponentially; used briefly to raise
  power, but sustained supercriticality on *prompt* neutrons alone (rather than the delayed
  neutron fraction reactors normally lean on for controllability) is how reactivity excursions
  happen.
(nuclear-power.com: https://www.nuclear-power.com/nuclear-power/reactor-physics/nuclear-fission-chain-reaction/reactor-criticality/ ;
Wikipedia "Criticality (status)": https://en.wikipedia.org/wiki/Criticality_(status) ;
Wikipedia "Prompt criticality": https://en.wikipedia.org/wiki/Prompt_criticality)

**Neutron moderators.** Fission releases *fast* neutrons (~2 MeV), but U-235's fission cross
section is much larger for *thermal* (slow, ~0.025 eV) neutrons. A moderator is a light-nuclei
medium that slows fast neutrons via elastic scattering without absorbing too many of them
(good "neutron economy"). Common moderators and their tradeoffs:
- **Light (ordinary) water (H₂O)**: cheap, abundant, doubles as coolant, but hydrogen has a
  non-trivial neutron absorption cross section — parasitic capture is high enough that
  light-water reactors need *enriched* uranium (rather than natural uranium) to sustain
  criticality.
- **Heavy water (D₂O)**: deuterium absorbs far fewer neutrons than protium, giving the best
  neutron economy of the three — heavy-water reactors (e.g. CANDU) can run on *natural*,
  unenriched uranium. Tradeoff is cost: separating deuterium from ordinary water is expensive.
- **Graphite**: also very low neutron absorption, also enables natural-uranium fuel, historically
  cheaper than heavy water at scale (used in early piles and RBMK reactors), but has its own
  issues (Wigner energy storage/release, flammability at high temperature — a contributing
  factor at Chernobyl).
(Wikipedia "Neutron moderator": https://en.wikipedia.org/wiki/Neutron_moderator ;
Energy Education: https://www.energyeducation.ca/encyclopedia/Neutron_moderator ;
Stanford "Graphite Moderation and the Wigner Effect": http://large.stanford.edu/courses/2026/ph241/benyas1/)

**Control rods** are rods of strong neutron-absorbing material inserted into or withdrawn from
the core to directly manage k-eff and thus reactor power:
- **Boron** (as B₄C or borated steel): high absorption cross section, low cost — common choice.
- **Cadmium**: excellent absorber but has thermal/high-temperature limitations restricting use.
- **Hafnium**: high absorption *and* good corrosion resistance in water, making it well suited
  to water-cooled reactor environments.
Withdrawing rods removes absorber from the core, pushing k-eff up (raising power); inserting
them adds absorber, pushing k-eff down (lowering power) — full insertion is a "scram" (emergency
shutdown), which stops the chain reaction quickly but does **not** stop decay heat (see §3).
(nuclear-power.com "Control Rods": https://www.nuclear-power.com/nuclear-power-plant/control-rods/ ;
Wikipedia "Control rod": https://en.wikipedia.org/wiki/Control_rod)

## 2. Real reactor types

**PWR (pressurized water reactor)**: water is both moderator and coolant. The **primary loop**
keeps water at very high pressure (~155 bar / 2250 psi) so it stays liquid even above 300°C;
this hot primary water passes through a steam generator, transferring heat (without mixing) to
a separate **secondary loop** of water that boils into steam and spins the turbine. Because the
primary and secondary water never mix, radioactivity stays confined to the primary loop and
turbine hall equipment is not directly irradiated.

**BWR (boiling water reactor)**: a single loop — water boils directly inside the reactor vessel
and the resulting steam goes straight to the turbine, no intermediate heat exchanger. Simpler
and marginally more thermally efficient, but the turbine is exposed to slightly radioactive
steam, complicating maintenance.
(Duke Energy PWR/BWR overview: https://nuclear.duke-energy.com/2012/03/27/pressurized-water-reactors-pwr-and-boiling-water-reactors-bwr ;
Wikipedia "Pressurized water reactor": https://en.wikipedia.org/wiki/Pressurized_water_reactor ;
Wikipedia "Boiling water reactor": https://en.wikipedia.org/wiki/Boiling_water_reactor)

**Scale, for sanity-checking mod numbers.** A large modern PWR such as the EPR is rated at
4,300 MWt (thermal) to produce 1,600 MWe (electrical) — roughly a **37% thermal efficiency** at
the high end. More typical operating LWRs run **33-35% thermal efficiency**: only about a
third of the fission heat becomes electricity: the rest is rejected as waste heat (cooling
towers/river/sea), the same Carnot-limited compromise that also caps the Thermal Generator's
coal-to-FE conversion. An 1,100 MWe four-loop PWR core (~3,300 MWt) typically holds around 193
fuel assemblies, i.e. roughly **17 MWt per assembly** as a ballpark figure.
(World Nuclear Association, reactor overview: https://world-nuclear.org/information-library/nuclear-fuel-cycle/nuclear-power-reactors/nuclear-power-reactors ;
IMechE nuclear power stations: https://www.imeche.org/policy-and-press/from-our-perspective/energy-theme/nuclear-power/about-nuclear-power/how-does-it-work/nuclear-power-stations ;
encyclopedie-environnement.org PWR overview: https://www.encyclopedie-environnement.org/en/zoom/pwr-pressurized-light-water-reactors/)

## 3. Meltdown/safety physics

**The key fact for grounding a failure mechanic**: shutting down the chain reaction (k < 1, or
even a full control-rod scram) does **not** stop the reactor from producing heat. Fission
products — the radioactive daughter nuclei left over from splitting U-235 — continue to undergo
beta and gamma decay, and that decay itself releases heat, independent of any ongoing chain
reaction. This is **decay heat** (a.k.a. afterheat). Immediately after shutdown it is roughly
**~7% of the pre-shutdown power level**, falling to about **1% after one hour** and **~0.5%
after one day**, following the combined decay of many fission products with half-lives ranging
from seconds to years. On a multi-gigawatt-thermal reactor, even 1-2% is tens of megawatts —
easily enough to melt fuel cladding if cooling stops entirely.
(nuclear-power.com "Decay Heat": https://www.nuclear-power.com/nuclear-power/reactor-physics/reactor-operation/residual-heat/decay-heat-decay-energy/ ;
whatisnuclear.com "What is afterglow/decay heat?": https://whatisnuclear.com/decay-heat.html)

**Meltdown mechanism**: a **loss-of-coolant accident (LOCA)** — coolant flow stops or coolant is
lost — removes the reactor's ability to carry decay heat away from the fuel. Even with the chain
reaction fully stopped, decay heat keeps accumulating in the fuel assemblies; without active or
passive heat removal, fuel and cladding temperatures climb until the fuel geometry fails
(cladding oxidation/hydrogen generation, fuel melting). This is why reactors require dedicated
decay-heat/residual-heat removal systems that must keep functioning *after* shutdown, not just
during power operation.
(nuclear-power.com decay heat page, above; Grokipedia "Nuclear meltdown": https://grokipedia.com/page/Nuclear_meltdown)

**Defense in depth**: rather than relying on one perfect barrier, real reactor safety stacks
multiple independent, redundant, and diverse layers (physical barriers — fuel cladding, reactor
vessel, containment building; redundant safety systems e.g. multiple independent emergency core
cooling trains; procedural/administrative controls; and offsite emergency response), such that
no single failure — human or mechanical — causes an accident, and failure of one layer doesn't
cascade into failure of the others (e.g. a fire-suppression failure shouldn't disable emergency
core cooling).
(NRC glossary "Defense in depth": https://www.nrc.gov/reading-rm/basic-ref/glossary/defense-in-depth ;
Wikipedia "Defense in depth (nuclear engineering)": https://en.wikipedia.org/wiki/Defense_in_depth_(nuclear_engineering) ;
risk-engineering.org: https://risk-engineering.org/concept/defence-in-depth)

## 4. Candidate design ideas (non-binding)

- **Moderator as a placed block/material choice**, not a slider: e.g. water, heavy water, and
  graphite blocks placed inside the multiblock structure could each carry a "moderation
  efficiency" and "parasitic absorption" pair mirroring the real tradeoffs in §1 (light water =
  cheap/available but needs enriched fuel; heavy water = best neutron economy but expensive to
  obtain/craft; graphite = cheap and available but real-world flammability/Wigner-energy risk
  could translate into a distinct, real-motivated failure mode instead of an arbitrary one).
- **Control rods as insertable/withdrawable multiblock components** with a 0-100% insertion
  value directly setting a k-eff-like multiplier on reaction rate — mirrors real reactor power
  control instead of an arbitrary on/off toggle, and gives a natural "scram" action (rods fully
  inserted, chain reaction halted, but see decay heat below).
- **A genuine primary/secondary loop distinction** (PWR-style) using the mod's existing
  fluid/energy pipe systems: a sealed high-pressure primary coolant loop through the core feeding
  a heat exchanger, and a separate secondary loop that actually spins the FE-generating turbine —
  reusing real safety logic (primary loop breach ≠ secondary loop contamination) as a real
  in-game failure category.
- **Decay heat as a genuine post-shutdown mechanic**: after the chain reaction stops (control
  rods fully inserted or fuel depleted), the reactor should keep emitting a shrinking-over-time
  heat/power output (e.g. modeled loosely on the real ~7% → ~1% → ~0.5% decay curve) that still
  requires active or passive cooling; failing to cool it is what triggers a meltdown state,
  rather than meltdown being tied to an arbitrary instantaneous "percent full" threshold.
- **Power output derived through EnergyScale, scaled down like the coal generator/solar panel
  were**: anchor a single fuel-rod/assembly component to a fraction of the real ~17 MWt/assembly
  figure from §2 (converted through 1 FE = 750 J), then apply the same kind of deliberate
  scale-down the Thermal Generator and Solar Panel already use (a real assembly's tens of MW is
  clearly too large for FE storage/transfer numbers already established) — keeping the *ratios*
  between fission, coal, and solar output physically honest even though the absolute numbers are
  compressed for gameplay, consistent with the mod's existing precedent of scaling real physical
  quantities down rather than inventing new ones.
