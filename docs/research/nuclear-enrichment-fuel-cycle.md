# Research: Uranium Enrichment & Nuclear Fuel Cycle

**Purpose:** ground the design of NovaCore's Nuclear Physics pillar (enrichment/fuel-cycle
mechanic) in real-world nuclear engineering. Pure research — no game values or code decided
here. Cross-reference: `src/main/java/com/novacore/energy/EnergyScale.java` (1 FE = 750 J,
coal anchor 24 MJ/kg).

## 1. The real fuel cycle, in order

1. **Mining** — uranium ore extracted by open-pit, underground, or in-situ leach (ISL)
   mining. Ore grades are typically well under 1% uranium by mass.
2. **Milling** — ore is crushed, ground, and chemically leached to concentrate the uranium,
   producing **yellowcake** (uranium oxide concentrate, U₃O₈), the form in which uranium is
   sold on the market.
3. **Conversion** — yellowcake is refined to remove impurities and converted into **uranium
   hexafluoride (UF₆)**, a compound that becomes a gas at a comparatively low temperature
   (~56 °C), which is required for the enrichment step.
4. **Enrichment** — UF₆ gas is spun in centrifuges (see §2) to raise the proportion of the
   fissile isotope U‑235 relative to U‑238.
5. **Fuel fabrication** — enriched UF₆ is converted to uranium dioxide (UO₂) powder, pressed
   and sintered into ceramic pellets, loaded into metal tubes (fuel rods), and grouped into
   fuel assemblies (see §3).
6. **Use in reactor** — fuel assemblies sit in the reactor core for several years, undergoing
   fission and gradually accumulating fission products that "poison" the reaction, at which
   point the fuel is "spent."
7. **Spent fuel → reprocessing or waste storage** — spent fuel is initially stored in cooling
   pools, then either: (a) **reprocessed** chemically to recover reusable uranium (~95%) and
   plutonium (~1%) for new fuel, though this increases total waste volume and doesn't
   eliminate the need for a repository; or (b) treated as waste and destined for deep
   **geological repository** disposal (the more common path, e.g. in the US).

Sources: [World Nuclear Association – Nuclear Fuel Cycle Overview](https://world-nuclear.org/information-library/nuclear-fuel-cycle/introduction/nuclear-fuel-cycle-overview),
[WNA – Conversion, Enrichment & Fabrication](https://world-nuclear.org/information-library/nuclear-fuel-cycle/conversion-enrichment-and-fabrication),
[WNA – Nuclear Essentials: How is uranium made into nuclear fuel?](https://world-nuclear.org/nuclear-essentials/how-is-uranium-made-into-nuclear-fuel),
[Congress.gov CRS – Considerations for Reprocessing of Spent Nuclear Fuel](https://www.congress.gov/crs-product/R48364),
[UCS – Nuclear Reprocessing: Dangerous, Dirty, and Expensive](https://www.ucs.org/resources/nuclear-reprocessing-dangerous-dirty-and-expensive)

## 2. Enrichment specifics

- **Natural uranium**: ~0.7% U‑235, ~99.3% U‑238.
- **LEU (low-enriched uranium, reactor-grade)**: 3–5% U‑235 — standard commercial power
  reactor fuel.
- **HEU (highly-enriched uranium)**: IAEA defines this as ≥20% U‑235. HEU has essentially no
  civilian power use.
- **Weapons-grade**: typically ≥90% U‑235 (a device can technically work down to ~80–85%
  with reduced yield/complexity).
- **Non-linear effort**: going from 0.7% → 20% takes roughly **90% of the total separative
  work** needed to reach 90%; the remaining climb from 20% → 90% takes only ~10% more
  effort. This is why the 20% HEU line is treated as the proliferation-risk threshold.

**Design flag:** the mod should almost certainly hard-cap enrichment well below the 20% HEU
line (e.g. LEU range only), both to stay grounded in realistic *civilian* nuclear engineering
and to avoid modeling weapons-grade material as a reachable player goal.

**Methods:**
- **Gas centrifuge cascades** (the modern/near-universal method): UF₆ gas is spun at high
  speed in tall rotating cylinders. The centrifugal force pushes the heavier U‑238 slightly
  outward, leaving a marginally higher concentration of U‑235 near the axis. Each individual
  centrifuge produces only a *tiny* enrichment increase, so machines are chained in
  **cascades** — the product of one stage feeds the next, with tails (depleted stream)
  recycled backward. Reaching commercial LEU (3–5%) typically requires **hundreds of
  centrifuge stages** in a cascade; one documented cascade design used 11 enriching + 6
  stripping stages to reach 3% from natural feed, illustrating how staged concentration
  compounds gradually rather than jumping directly to target purity.
- **Gaseous diffusion** (older, largely retired method): UF₆ gas is forced through
  porous membranes; U‑235-bearing molecules diffuse marginally faster. Far more
  energy-intensive.

**Energy cost per separative work unit (SWU)** — a useful real anchor for per-stage game
costs:
- Modern gas centrifuge plants: **~50 kWh/SWU** (some sources cite <50 kWh/SWU).
- Gaseous diffusion: **~2,400–2,500 kWh/SWU** — roughly 40–50× less efficient than
  centrifuges.
- Example scale: a 20,000 kg-SWU/year centrifuge plant draws about 600 kW continuous
  electrical power.

Sources: [WNA – Uranium Enrichment](https://world-nuclear.org/information-library/nuclear-fuel-cycle/conversion-enrichment-and-fabrication/uranium-enrichment),
[Center for Arms Control and Non-Proliferation – Uranium Enrichment: For Peace or for Weapons](https://armscontrolcenter.org/uranium-enrichment-for-peace-or-for-weapons/),
[Union of Concerned Scientists – Fissile Materials Basics](https://www.ucs.org/resources/fissile-materials-basics),
[Wikipedia – Enriched uranium](https://en.wikipedia.org/wiki/Enriched_uranium),
[Wikipedia – Separative work units](https://en.wikipedia.org/wiki/Separative_work_units),
[GlobalSecurity.org – Gas Centrifuge Uranium Enrichment](https://www.globalsecurity.org/wmd/intro/u-centrifuge.htm)

## 3. Fuel fabrication basics & energy density

**Physical form:**
- **Pellets**: sintered UO₂ ceramic cylinders, roughly **1 cm tall × ~8 mm diameter**, about
  **10 g** each (~8.8 g of uranium content).
- **Rods**: pellets stacked inside a **zirconium-alloy** (e.g. Zircaloy, ZIRLO, M5) cladding
  tube chosen for its very low neutron-absorption cross-section; a typical rod is **~4 m
  long, ~1 cm diameter**, with a helium-filled gap between pellet and cladding for heat
  transfer.
- **Assemblies**: many rods (commonly on the order of 200+ in a PWR assembly) bundled
  together with spacer grids and end fittings.

**Energy density (the sanity-check vs. the mod's coal anchor):**
- Coal (per NovaCore's existing `EnergyScale` anchor): **~24 MJ/kg** (~8 kWh/kg).
- A single ~10 g LWR fuel pellet (8.8 g uranium, enriched) releases about **35,000 MJ**
  of heat — equivalent to roughly **1.3 tons of coal**, ~250 gallons of oil, or ~34,000
  cubic feet of natural gas.
- Per kilogram, fully fissioned U‑235 is on the order of **~83,000,000 MJ/kg**.
- One frequently-cited real-world figure: 1 kg of natural uranium, after enrichment and use
  in a light-water reactor, yields about **45,000 kWh of electricity** — equivalent to
  ~10,000 kg of oil or ~14,000 kg of coal.
- **Bottom line multiplier vs. NovaCore's coal anchor (24 MJ/kg):** enriched uranium fuel is
  roughly **1–3 million times more energy-dense than coal by mass** (figures across sources
  range ~2–3 million×, with some framing it as low as ~16,000× when only using the natural
  0.7% U‑235 fraction without enrichment/breeding credit — the range depends on whether
  enrichment concentration and full fission utilization are counted). This is the key number
  to reconcile against `EnergyScale.JOULES_PER_FE` when the reactor mechanic is designed —
  a literal 1:1 scaling of real energy density would make nuclear fuel wildly
  disproportionate to every other game system, so the eventual design will need a deliberate,
  documented "game energy density" that departs from the literal ratio in scale (while still
  being *derived* from it, per the mod's existing philosophy for the Thermal Generator).

Sources: [WNA – Nuclear Fuel and its Fabrication](https://world-nuclear.org/information-library/nuclear-fuel-cycle/conversion-enrichment-and-fabrication/fuel-fabrication),
[nuclear-power.com – Fuel Pellets](https://www.nuclear-power.com/nuclear-power-plant/nuclear-fuel/fuel-assembly/fuel-pellets/),
[whatisnuclear.com – Energy equivalents of one fuel pellet](https://whatisnuclear.com/calcs/energy-equivalents-of-one-fuel-pellet.html),
[European Nuclear Society – Fuel comparison](https://www.euronuclear.org/glossary/fuel-comparison/)

## 4. Candidate design ideas (not a decision)

- **Cascade machine block, multi-pass:** a single "Centrifuge" block that a player must run
  UF₆-equivalent gas/fluid through repeatedly (each pass = one simulated cascade stage),
  incrementing a tracked `enrichment%` float stored on the item/fluid stack, converging
  slowly toward a capped target the way real staged cascades approach LEU asymptotically
  rather than jumping straight there.
- **Multi-block cascade, stage count = building investment:** several centrifuge blocks
  physically chained (mirroring real cascades), where each additional block in the chain
  increases the enrichment ceiling reachable per unit time — visually and mechanically
  reinforcing that enrichment requires *staged infrastructure*, not a single powerful
  machine.
- **Real-SWU-derived energy cost per stage:** price each enrichment "pass" in FE using the
  centrifuge's real ~50 kWh/SWU figure converted through `EnergyScale` (750 J/FE), so
  higher enrichment tiers cost predictably more energy per stage, and an optional "gaseous
  diffusion" early-game alternative could exist at ~40–50× the FE cost per stage as a
  historically-accurate, deliberately-inefficient option.
- **Hard-capped enrichment ceiling:** cap the tracked `enrichment%` well below the 20% HEU
  line (e.g. 5–8% max) regardless of passes invested, both for realism (mod scope is a power
  reactor, not a weapons program) and to sidestep the design/ethical complexity of modeling
  weapons-grade material.
- **Non-linear cost curve:** make each successive percentage point of enrichment cost more
  energy/time than the last (mirroring the real non-linear SWU effort curve where 0.7%→20%
  is ~90% of the work to 90%), so early LEU-range enrichment is relatively cheap but pushing
  toward the cap gets steeply more expensive — discouraging players from treating enrichment
  as a trivial early-game step.
