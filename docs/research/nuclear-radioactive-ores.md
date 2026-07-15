# Research: Real-World Radioactive Elements & Ores (Nuclear Physics Pillar)

Status: reference material for design discussion. Not a spec. No implementation decisions made here.

## 1. Real radioactive elements/ores

### Uranium

- **Crustal abundance**: ~2.7 ppm average, comparable to tin or zinc; ~40x more common than silver, ~500x more common than gold. Uranium is not intrinsically "exotic" — it's just rarely *concentrated*. [World Nuclear Association](https://world-nuclear.org/information-library/nuclear-fuel-cycle/uranium-resources/supply-of-uranium)
- **Economic ore grade**: mining is generally only viable above ~750 ppm (0.075%), with typical operating mines around 0.10%+ U (1,000+ ppm) — i.e., ore must be concentrated ~400x above crustal background before it's worth digging. High-grade deposits (Athabasca Basin, Canada) can exceed 20% U in rare zones. [Uranium ore - Wikipedia](https://en.wikipedia.org/wiki/Uranium_ore)
- **Uraninite / pitchblende** (UO₂/U₃O₈): the primary uranium ore mineral, 50–80% uranium by weight in high-grade massive form — one of the richest ore minerals known for any metal. Forms in igneous/metamorphic and hydrothermal vein deposits (e.g., unconformity deposits in the Athabasca Basin, pegmatites). [Uraninite - Wikipedia](https://en.wikipedia.org/wiki/Uraninite), [Pitchblende - Britannica](https://www.britannica.com/science/pitchblende)
- **Carnotite** (K₂(UO₂)₂(VO₄)₂·3H₂O): a secondary, low-temperature uranium-vanadium-potassium mineral, ~53% U in pure form but found disseminated at much lower grades (historic Colorado Plateau "Uravan belt" ore averaged only ~0.24% U₃O₈). Forms in sandstone via groundwater "roll-front" deposits — uranium-bearing groundwater percolates through porous sandstone and precipitates uranium oxides on contact with organic material (fossil wood, etc.). This is a fundamentally different, near-surface sedimentary geology from uraninite's igneous/hydrothermal origin. [Carnotite - Wikipedia](https://en.wikipedia.org/wiki/Carnotite), [USGS - Colorado Plateau sandstone deposits](https://www.usgs.gov/publications/uranium-bearing-sandstone-deposits-colorado-plateau)
- **Radioactivity**: natural uranium is essentially pure alpha-decay material (U-238, U-235, U-234). Specific activity of U-238 alone is very low (~3.4×10⁻⁷ Ci/g, half-life 4.47 billion years) — alpha particles are stopped by a sheet of paper or a few cm of air. The real hazard from uranium ore isn't the uranium itself but its decay-chain daughters (see Radium/Radon below) and internal exposure if ore dust is inhaled/ingested. [nuclear-power.com](https://www.nuclear-power.com/nuclear-power-plant/nuclear-fuel/uranium/uranium-238/decay-half-life-uranium-238/)

### Thorium

- **Crustal abundance**: ~10–12 ppm, roughly 3–4x more abundant than uranium — thorium is genuinely the more common of the two. [Occurrence of thorium - Wikipedia](https://en.wikipedia.org/wiki/Occurrence_of_thorium)
- **Ore mineral**: monazite (a rare-earth phosphate), typically found as a minor accessory mineral concentrated in placer "black sand" deposits (beach/river sands) via density sorting. Monazite sand concentrates run ~6–12% ThO₂ after processing (raw black sand monazite content lower, ~3–22% variable by deposit). Major deposits: India (Kerala), Brazil, Australia. [Advanced Techniques for Thorium Recovery - MDPI](https://www.mdpi.com/2076-3417/15/21/11403)
- **Radioactivity**: Th-232 is an essentially pure alpha emitter with an extremely long half-life (~14 billion years, over 3x the age of Earth), making it even less intensely radioactive per atom than uranium. Its decay chain (via Ra-228, not Ra-226) is a separate chain from uranium's. [Thorium-232 - Wikipedia](https://en.wikipedia.org/wiki/Thorium-232)

### Other notable elements (decay products, not primary ore)

- **Radium (Ra-226)**: never mined directly as its own ore — it occurs in uranium ore only in secular equilibrium as a U-238 decay daughter, at roughly 1 part in 10¹¹ by mass (historically, processing hundreds of tons of uranium ore yielded about 1 gram of radium). Half-life 1,600 years; decays by alpha emission into radon-222 gas. [Radium and radon - Grokipedia summary of standard refs](https://grokipedia.com/page/Radium_and_radon_in_the_environment)
- **Polonium (Po-210)**: a trace, short-lived (138-day half-life) daughter far down the U-238 chain, alpha emitter, decays to stable Pb-206. Not something ever mined — only relevant as a late-chain decay product for flavor/mechanics, not as its own "ore." [Polonium-210 - Wikipedia](https://en.wikipedia.org/wiki/Polonium-210)
- **Full U-238 decay chain** (for reference, in case later mechanics model decay chains): U-238 → Th-234 → Pa-234 → U-234 → Th-230 → Ra-226 → Rn-222 (gas) → Po-218 → Pb-214 → Bi-214 → Po-214 → Pb-210 → Bi-210 → Po-210 → Pb-206 (stable). Half-lives span from 4.47 billion years (U-238) down to 164 microseconds (Po-214). [Uranium Decay Series](https://sciencereader.com/glossary/uranium-decay-series/)

## 2. Real-world sanity check for ore generation rarity

- Iron in Earth's crust averages ~5.6% (56,300 ppm) — roughly **20,000x more abundant** than uranium's ~2.7 ppm crustal average, and uranium ore bodies additionally require ~400x local geochemical concentration (roll-front leaching, hydrothermal vein deposition, etc.) before they're minable at all. This is a much steeper rarity/concentration curve than iron, which is broadly disseminated and needs comparatively little upgrading.
- Uranium's *crustal* abundance is actually closer to tin/zinc (i.e., not dramatically rarer than common industrial metals) — its real-world "rarity" as a resource is a mining/geology problem (needing rare concentrating processes: roll-front sandstone deposits, unconformity-related hydrothermal veins, pegmatites), not scarcity of atoms. This suggests uranium ore, if grounded in reality, should generate via a *specific geological process/biome logic* (e.g., tied to sandstone-type strata, or rare vein-type deposits) rather than uniform random scatter like iron — echoing how tech mods often gate uranium behind specific stone/biome conditions rather than pure Y-level rarity.

## 3. Isotopic composition (for future enrichment mechanics)

Natural uranium, by weight:
- **U-238**: 99.284%
- **U-235**: 0.711% (the fissile isotope; this is what enrichment increases)
- **U-234**: 0.0055% (trace, itself a decay daughter of U-238)

[Natural uranium - Wikipedia](https://en.wikipedia.org/wiki/Natural_uranium), [World Nuclear Association - Enrichment](https://world-nuclear.org/information-library/nuclear-fuel-cycle/conversion-enrichment-and-fabrication/uranium-enrichment)

This ~0.7% U-235 fraction is why enrichment (reactor-grade ~3–5%, weapons-grade ~90%+) is a real, energy-intensive industrial process — relevant later if the mod wants a centrifuge/enrichment mechanic gated behind meaningful throughput rather than a single instant conversion.

## 4. Candidate design ideas (not a decision)

- **Raw ore nearly inert, refined product mildly active**: real uraninite/pitchblende is only mildly radioactive in the hand — alpha emissions are blocked by skin/paper, and reported dose rates for typical mineral specimens are on the order of tens of µSv/hr at contact (compare: annual background dose is ~2,000–3,000 µSv/yr). A raw ore block could be flavor-radioactive (minor player effect only at prolonged close contact) while purified/processed material (yellowcake analog, fuel pellets) is where meaningful radiation mechanics kick in — mirroring real health-physics practice where raw ore is handled with minimal PPE but processed/concentrated material requires real shielding.
- **Distinct worldgen logic per ore type, matching real geology**: uraninite-analog as a rare vein/pocket tied to deep stone or granite-like blocks (echoing hydrothermal/pegmatite origin), carnotite-analog as a low-grade, more-spread-out find tied to sandstone-type blocks (echoing roll-front sedimentary origin) — giving two ores with different rarity/yield tradeoffs instead of one generic "uranium ore."
- **Thorium as the "common but underused" resource**: since thorium is really 3-4x more abundant than uranium, it could generate more frequently than uranium ore but yield a material that's inert until run through a separate future process (analogous to thorium needing neutron irradiation to breed into fissile U-233) — giving early-game abundance without early-game payoff, gated toward later tech.
- **Radium/radon as emergent decay byproducts, not mineable blocks**: rather than a "radium ore," radium/radon could exist only as a decay/byproduct mechanic from processing or storing uranium ore over time (matching that radium never occurs as its own primary deposit, only as a U-238 daughter at ~1 part in 10¹¹) — reserving it for a later decay-chain mechanic instead of a placeable ore block.
- **Natural isotope mix baked into raw material, enrichment as a real bottleneck**: raw uranium items could represent the natural 99.3%/0.7% U-238/U-235 mix and be largely inert as reactor fuel until processed through an enrichment chain, giving a concrete real-numbers hook for a future centrifuge mechanic rather than raw ore being usable directly.
