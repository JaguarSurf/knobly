# Spec: call statistics (Statistics tab and Home)

- **Status:** implemented on Android (iOS: see [platforms.md](../platforms.md))
- **Date:** 2026-11-14

## Goal

Show how many calls Knobly rejected over time and which filters did the work, so the user sees the value of the filters at a glance.

## Data

- Computed from the call history ([call-history.md](call-history.md)), which keeps rejected calls for **3 years**. Nothing else is stored.
- Aggregated in the phone's **local time zone** (days, weeks and daylight saving time follow the phone's clock). A call at 23:30 local time counts on that day, even if it is already the next day in UTC.
- Updated live: a call rejected while the tab is open shows up immediately.

## Statistics tab

Follows the approved mockup:

- Period selector: **Hora**, **Dia** (default), **Semana**, **Mês**, **Ano**. Each shows a fixed window ending with the current bucket:

  | Period | Window | Buckets | Labels |
  |---|---|---|---|
  | Hora | last 24 hours | 24 hours | `14h` (every few hours) |
  | Dia | last 7 days | 7 days | `Seg`, `Ter`… |
  | Semana | last 8 weeks | 8 weeks, starting on Monday | start date, `9/11` |
  | Mês | last 12 months | 12 months | `Jan`, `Fev`… |
  | Ano | last 5 years | 5 years | `2026` |

  With 3 years of history, the oldest years of the **Ano** view are empty.
- **Total** of rejected calls in the window ("18 rejeitadas nos últimos 7 dias") and a smooth line chart with one point per bucket. Labels that would overlap are skipped at a regular step, always keeping the current bucket. Screen readers get the total and the period.
- **Por filtro**: rejected calls of the window per filter, most first, with a bar relative to the largest: "Fora dos contatos", each custom rule ("Telemarketing · 0303", or "Começa com 0304" without a nickname; named as on its latest rejection) and "Regras excluídas".
- Empty window: total 0, a flat line, and "Nenhuma chamada rejeitada neste período."

## Home (control center)

As in the Home mockup:

- While call filtering is on, the calls card shows "**N** hoje" and "**N** em 7 dias".
- The latest rejected call (number, "Rejeitada hoje/ontem às 14:32" or the date), opening the History tab.

## Implementation notes

- `:domain:stats` (`CallStatistics`) aggregates in memory: three years of rejected calls are a few thousand rows at most, and doing it in Kotlin keeps time zone handling correct and testable. A pre-aggregated table can come later if needed.
- The chart is a Compose Canvas component of the design system (`KnoblyLineChart`), not a chart library.
