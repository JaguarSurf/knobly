# ADR-0004: Generic event-based rule engine

- **Status:** proposed
- **Date:** 2026-11-13

## Context

Knobly starts by filtering calls, but it should evolve into other utilities and controls (SMS, notifications, etc.). If the rule engine were coupled to calls, each new feature would require a new engine.

## Decision

The engine (`:domain:rules`) evaluates generic **events** (`Event`). `IncomingCall` is the first event type. Each rule implements `Rule<E : Event>`, using Strategy for the rule type and Chain of Responsibility for evaluation by priority. Rules are persisted as `type + params JSON`.

## Alternatives considered

- **Call-specific engine:** simpler at first, but would require a rewrite for the second feature.
- **Custom DSL or rule language:** too flexible for the MVP; may be revisited in the future.

## Consequences

- New rule and event types are added without changing the engine.
- Exporting and importing rules becomes trivial (JSON).
- A bit more abstraction at the start of the project.
