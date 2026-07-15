# Research: Radioactive Decay Physics (for the Nuclear Physics pillar)

Pure research doc, no implementation. Grounds a future "radioactive decay" mechanic in
real physics, following NovaCore's rule that nothing should extrapolate beyond reality.

## 1. Core physics

**Half-life (t½)**: the time for half of a sample of a radioactive isotope to decay. It is a
statistical property of the nuclide — every atom has a constant per-tick probability of
decaying, independent of the atom's age ("memorylessness"). This produces exponential decay,
not linear decay.

**Exponential decay law**:

```
N(t) = N0 * 2^(-t / t½)          (half-life form)
N(t) = N0 * e^(-λt)              (decay-constant form, λ = ln(2) / t½)
```

`N0` = initial quantity, `N(t)` = quantity remaining at time `t`, `λ` = decay constant
(probability of decay per unit time for a single atom). Activity (decays per second,
measured in becquerels) is `A(t) = λ * N(t)`, so activity decays with the same exponential
curve as quantity. Reference: https://en.wikipedia.org/wiki/Radioactive_decay and
https://en.wikipedia.org/wiki/Exponential_decay

**Three main decay types**:

| Type | What is emitted | Typical shielding | Relative danger |
|---|---|---|---|
| Alpha (α) | A helium-4 nucleus (2 protons + 2 neutrons) ejected from the nucleus | Stopped by a sheet of paper or a few cm of air; cannot penetrate the dead outer layer of skin | Safest external exposure (can't reach living tissue from outside), but by far the most dangerous if inhaled or ingested — short range means all its energy dumps into a tiny volume of living cells. Radiation-weighting factor ~20x beta/gamma for equal absorbed dose. |
| Beta (β) | A high-speed electron (β⁻) or positron (β⁺) emitted when a neutron converts to a proton (or vice versa) | Stopped by a few mm to ~1 cm of aluminum, or plastic/glass; can penetrate skin and cause burns | Moderate external hazard (skin/eye damage), moderate internal hazard if ingested — less densely ionizing than alpha. |
| Gamma (γ) | High-energy electromagnetic photons emitted as the nucleus relaxes after alpha/beta decay | Requires several cm of lead or tens of cm of concrete/water to substantially attenuate (attenuation is exponential with thickness, described by half-value layers, not a hard "stop") | Worst external hazard — penetrates deep into the body and passes through most materials; lower relative biological effectiveness per unit dose than alpha, but its range makes whole-body/organ exposure likely. |

Sources: https://www.nrc.gov/about-nrc/radiation/health-effects/radiation-basics.html ,
https://en.wikipedia.org/wiki/Alpha_particle , https://en.wikipedia.org/wiki/Beta_particle ,
https://en.wikipedia.org/wiki/Gamma_ray , https://radetco.com/your-complete-guide-materials-that-block-radiation/

## 2. Real half-lives for isotopes likely relevant to the mod

| Isotope | Half-life | Decay mode | Relevance |
|---|---|---|---|
| Uranium-238 | ~4.468 billion years | alpha | primary reactor fuel isotope (99.3% of natural U) |
| Uranium-235 | ~703.8 million years | alpha | fissile isotope (0.72% of natural U), enrichment target |
| Plutonium-239 | ~24,110 years | alpha | breeder/fuel-cycle product, weapons-relevant |
| Caesium-137 | ~30.17 years | beta (→ Ba-137m, gamma) | classic fission product, dominant "nuclear waste" contamination isotope (Chernobyl/Fukushima) |
| Strontium-90 | ~28.8–28.91 years | beta | fission product, bone-seeking biological hazard |
| Iodine-131 | ~8.02 days | beta/gamma | short-lived fission product, dominant acute hazard right after a reactor accident, then vanishes |

Sources: NNDC Chart of Nuclides https://www.nndc.bnl.gov/nudat3/ ,
https://en.wikipedia.org/wiki/Uranium-238 , https://en.wikipedia.org/wiki/Uranium-235 ,
https://en.wikipedia.org/wiki/Caesium-137 , https://en.wikipedia.org/wiki/Strontium-90 ,
https://www.cdc.gov/radiation-emergencies/hcp/isotopes/uranium-235-238.html ,
IAEA recommended half-lives https://nds.iaea.org/xgamma_standards/halflives1.htm

Note the spread: I-131 decays meaningfully in days, while U-238 barely decays over the
age of the universe. Any in-game system covering both ends of this table with one time
scale is going to be strained (see §3).

## 3. The compression problem

A Minecraft day is 24,000 ticks ≈ 20 real-world minutes (1 tick = 1/20 s at 20 TPS). Real
half-lives span from 8 days (I-131) to 4.5 billion years (U-238) — about 11 orders of
magnitude. There is no single linear real-time-to-game-time ratio that keeps *all* of these
isotopes both (a) numerically faithful to their real decay constant and (b) observable by a
player within a reasonable play session. Scaling so U-238 decays visibly in-game (say, over
a few Minecraft days) would make I-131 decay in a fraction of a tick; scaling so I-131 decays
over a sensible number of ticks would put U-238's half-life at a timescale no player will
ever reach.

This is a *unit-scale* problem, not a physics problem, and it's the same shape of problem
NovaCore already solved for the Energy pillar: `EnergyScale.JOULES_PER_FE` (see
`src/main/java/com/novacore/energy/EnergyScale.java`) fixes 1 FE = 750 J as a single global
conversion constant, anchored to a real physical reference (coal's ~24 MJ/kg calorific
value) so every later energy mechanic derives from real numbers instead of being hand-picked.
Compressing decay time the same way — one global "real seconds per game tick" (or per
Minecraft day) constant applied uniformly to every isotope's real λ — preserves the *relative*
physics faithfully: an isotope with 1000x the half-life of another still decays 1000x slower
in-game, activity still follows N(t) = N0 · 2^(-t/t½) exactly, and specific-activity /
dangerousness relationships between isotopes are undistorted. What's lost is only the
absolute real-world time axis, exactly as FE loses the absolute real-world Joule axis while
preserving relative energy costs. This is the well-established approach in serious
science/education simulators — time-compression (or "time-lapse") factors are used
explicitly so decay remains visible on human timescales without altering the underlying
exponential law, e.g. general-audience half-life simulators like PhET's "Alpha Decay" /
"Half-Life" sims compress the visible timescale while keeping the decay law and per-isotope
constants real: https://phet.colorado.edu/en/simulations/alpha-decay and
https://phet.colorado.edu/en/simulations/beta-decay . The same logic underlies "geologic time
compression" used in planetarium/orrery software and in nuclear-engineering training
simulators that fast-forward spent-fuel cooling curves — the decay constant itself is never
altered, only the clock it's evaluated against.

The open design question for a future conversation is *which* global compression factor(s)
to pick (a single factor for all isotopes vs. tiered factors for gameplay-relevant vs.
flavor-only isotopes) and how it interacts with existing NovaCore time constants (ticks/day,
existing Energy pillar rates). This doc intentionally does not decide that.

## 4. Candidate design ideas (not a decision)

- **Single global time-compression constant** (`DecayScale`, mirroring `EnergyScale`):
  define `TICKS_PER_REAL_SECOND_EQUIVALENT` once, anchored to a chosen real reference
  (e.g. "1 game day = X real half-lives of isotope Y"), and derive every isotope's in-game
  λ_game = λ_real · compressionFactor from its real half-life, keeping all isotopes internally
  consistent relative to each other.
- **Per-isotope decay constant table driven by the same global factor**: store each isotope's
  real t½ (as in §2) as data, compute λ_game at load time via the single compression constant,
  and use standard N(t) = N0·2^(-t/t½_game) per-tick/per-check math — no isotope-specific
  hacks, just the real formula fed a compressed but uniform time axis.
- **Tiered compression bands** (still numerically derived, not hand-tuned per isotope):
  bucket isotopes by real half-life magnitude (short-lived fission products like I-131 vs.
  long-lived fuel/waste like Pu-239/U-238) and apply a different but still fixed, documented
  compression ratio per band, similar to how some simulators separate "human-scale" and
  "geologic-scale" clocks — the risk is this reintroduces an extra manual constant, so it
  should only be used if a single global factor proves unworkable across the mod's whole
  isotope roster.
- **Stochastic per-tick decay check using the real λ_game**: instead of computing N(t)
  directly, roll a per-atom/per-unit decay probability each tick (`p = 1 - e^(-λ_game·Δt)`),
  which matches how real detectors/Geiger counters behave probabilistically and would let
  a "nuclear waste" block visibly emit discrete decay events rather than smoothly draining.
- **Activity-first design (Bq-equivalent) instead of quantity-first**: expose isotopes to the
  player primarily via their activity (decay rate / radiation output, relevant for hazard and
  detector mechanics) rather than remaining mass fraction, since activity is what actually
  drives gameplay-relevant danger and shielding math, and it already falls out of the same
  N(t) formula (A = λN) once the compression constant is fixed.
