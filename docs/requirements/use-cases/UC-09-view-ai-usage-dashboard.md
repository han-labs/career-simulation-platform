# UC-09 — View AI usage dashboard

> **Status:** SELECTED / present in current report
> **Actor:** Administrator
> **Report-selected User Story mapping:** US-10
> **Specification source:** Current 2026-10-22 report §1.5
> **Module owner:** Người 2

## Trigger
The actor wants to inspect AI workload and operational results.

## Description
Reports request volume, latency, measured or estimated cost, and failed/refused outcomes without exposing Student message bodies.

## Preconditions
The actor is an authenticated Administrator.

## Postconditions
The actor views the authorized AI usage summary for the selected interval.

## Main flow
- 1. The actor opens AI usage dashboard and selects a supported time range or filter.
- 2. The system aggregates eligible AI execution records and displays request count, average latency, available cost and failed/refused counts.

## Alternative flows
- 1a. The actor changes or clears a filter. The system recalculates the authorized summary and returns to Main Flow Step 2.
- 2a. No eligible requests exist. The system displays “No AI usage records are available for this period.” The actor may change the filter at Main Flow Step 1.

## Exception flows
- 2.1. If an AI cost or token measurement is unavailable, the system displays “Not available” for that measure rather than a fabricated zero. The dashboard remains accessible.
- 2.2. If the usage query fails, the system displays “AI usage data could not be loaded. Please try again.” The actor remains on the dashboard and may retry Main Flow Step 2.

## Source alignment / implementation notes
- Preserve the source specification; verify exact API/UI behavior and automated tests against current `main` before marking as accepted.

## Verification tracking
- API/component: [AgentUsageController.java](../../../backend/src/main/java/edu/hcmute/careersim/guidance/agent/controller/AgentUsageController.java); [AgentUsagePage.jsx](../../../frontend/src/features/admin/agentPlatform/AgentUsagePage.jsx).
- Automated tests: [StudentAgentIntegrationTest.java](../../../backend/src/test/java/edu/hcmute/careersim/guidance/agent/StudentAgentIntegrationTest.java). Existing dated results are in local P1–P6 completion reports; **not rerun here**, not full UC acceptance.
- Browser/UAT: **PENDING until observed**
- Human reviewer/sign-off: **PENDING**

## Current repository comparison (2026-10-22)

Safe aggregate projection distinguishes known/unknown/partial/not-applicable metrics. Mock observations and estimated costs are not provider billing or production benchmarks.

Checked main `5486f9f` and matching application source; source flows above are retained. See [alignment/provenance](../REPORT_CODE_ALIGNMENT_2026-10-22.md), [current catalogue](README.md) and [official allocation](../../TEAM_ASSIGNMENTS.md). Supplementary and deferred scope is not expanded by this comparison.
