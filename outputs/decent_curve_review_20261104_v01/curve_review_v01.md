# Decent curve review — 2026-11-04

## Finding

The previous implementation only partly followed Decent's plotted examples. Adaptive v2 already used a seven-second flow probe and a pressure ceiling, which follows its stated control idea. Its plotted example is one observed shot, however, and shows a pressure peak followed by a short extraction; its axes display measured pressure and flow, not just the active target.

The LRv2 pressure curve was directionally similar but the implementation omitted the source's flow-triggered handoff. Decent's example holds about 3 bar through preinfusion, rises to about 9 bar, then declines gradually. Damian's LRv2 source exits the decline when flow exceeds 2.8 mL/s, holds 3 bar until that flow threshold is crossed, and then uses a 2.5 mL/s flow target. The sample graph is a 33-second shot; its table reports pressure 3.1→9.1→7.8 bar and flow 0.2→2.2 mL/s during extraction.

## GeeFlow changes

- Adaptive v2 remains an app-controlled flow profile: it learns the fastest measured flow below an 8.8 bar LITA pump-pressure ceiling, then backs off at the ceiling. LITA pump telemetry is not Decent's coffee-pressure measurement.
- LRv2 now includes the source's `flow > 2.8 mL/s` exits, 3 bar hold, and 2.5 mL/s flow phase. GeeFlow changes `FREE_VAR_MODE` at live phase transitions for flow-capable models so the transition follows telemetry instead of a fixed elapsed time.
- Wendougee's product page distinguishes BA/LA dual-pump flow-profiling models from BR/LR single-pump models. GeeFlow keeps flow unavailable for BR/LR.
- GeeFlow's profile model still lacks per-phase temperature settings. These starters use the no-scale 60 mL pumped-water stop; this is not equivalent to Decent's 42 g cup-weight stop.

The live mode-register handoff is implemented from GeeFlow's existing register map and the user's direction. It has not been validated on the physical machine in this turn. Tests were not run.

## Primary sources

1. Decent Espresso, “First Decent Espresso: Dial-In Guide,” sections Adaptive v2 and Londonium (LRv2), accessed 2026-11-04: https://decentespresso.com/first_decent_espresso
2. Decent Espresso, “Damian's LRv2.tcl,” official `de1app` repository, accessed 2026-11-04: https://github.com/decentespresso/de1app/blob/main/de1plus/profiles/Damian%27s%20LRv2.tcl
3. Wendougee, “LITA Semi-Automatic Espresso Machine,” product variants and flow-profiling description, accessed 2026-11-04: https://shop.wendougee.com/collections/lita-semi-automatic-espresso-machine
4. Decent's page states its text and photos are reusable under CC BY 4.0: https://creativecommons.org/licenses/by/4.0/

## Archived source record and measurements

- `source_texts/first_decent_espresso_record_v01.md` preserves the relevant source sections, short excerpts, links, and access date.
- `references_v01.csv` maps the numeric curve/profile values to their source locations and marks which values are recipe settings versus observed graph measurements.
