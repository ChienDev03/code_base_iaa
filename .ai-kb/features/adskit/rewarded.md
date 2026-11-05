# Rewarded and fullscreen lifecycle

Back to [ads integration](../adskit.md).

## Outcome ownership

Normalize the selected SDK's earned, dismissed, unavailable and failed outcomes without inventing
SDK enum names. Only its confirmed earned callback authorizes a reward. Closing an ad, receiving
an impression or timing out is not evidence of a reward.

One owner claims each fullscreen operation and delivers one terminal continuation. Handle skipped
shows, load/show failure, invalid hosts, cancellation and owner destruction. Never invoke an old
screen's navigation or reward callback after its context is invalidated.

## Feature integration

The feature checks eligibility and quota, claims its action, requests the ad and applies the
approved continuation. If reward delivery requires a feature API, keep that separate from payment
entitlement: removing backend subscription verification does not remove unrelated feature APIs.
Define idempotency and retry for a reward already earned before a feature commit fails.

Confirm unavailable/disabled placement behavior, wait duration, retry, Back/dismiss, background
handling and ordering relative to result UI before implementation. Do not inherit a previous
product's timeouts, cooldowns, automatic paywall fallback or navigation sequence.

## Loading and interruption

Bound preload work, retries and pending show waits. If single-use opportunities are needed, key
them by stable feature context and reject callbacks after replacement, timeout or consumption.
A timeout applies to waiting for presentation, not an ad already being displayed. A continuation
may proceed without an interstitial only according to the feature's approved failure policy.

Re-evaluate consent, shared premium eligibility and usable host state before showing. Eligibility
comes from the [premium repository](../premium.md), without backend subscription status calls.
Cancellation and terminal cleanup must not cancel another owner's newer operation.

## Verification cases

Cover duplicate taps, unresolved configuration, disabled placements, pending load, no fill, timeout,
reward then dismissal, dismissal without reward, background, host destruction, entitlement change,
late callbacks and feature commit retry. Keep approved analytics tied to actual SDK outcomes.
These are verification cases, not claims of executed tests; obtain authorization before test runs.

## Reference caller flow

```text
accepted feature action
→ synchronously claim in-flight owner
→ resolve placement configuration
→ check current entitlement, consent and host
→ show / bounded load wait
→ SDK earned callback (rewarded only)
→ dismissal or terminal failure
→ feature continuation once
→ release owner
```

The reference SDK exposes `RewardedAdOutcome` with `EARNED`, `DISMISSED`, `UNAVAILABLE`. Check
that API in the selected artifact; keep app mapping in one adapter instead of duplicating SDK enums.
Its reward notification follows dismissal before the terminal callback. If the chosen SDK orders
callbacks differently, adapt explicitly while preserving one reward and one continuation.

### Example single-use interstitial opportunity

Reference API: `preload(placementId, keepPreload = false, requestKey = contextId)` where supported.
Use a stable example content/action ID as `contextId`, not a former product's daily-cycle field.

| Event | Reference behavior |
| --- | --- |
| Visible eligible content | Start or reuse one keyed preload |
| Feature submit fails | Keep draft; do not show a success-path interstitial |
| Feature submit succeeds | Reuse/wait for matching preload, then show if still eligible |
| Wait expires or load fails | End pending show and follow approved skip/continue UX |
| Ad already visible | Wait for SDK dismissal; a load timeout cannot close it |
| Context replaced / owner destroyed | Invalidate old continuations and keyed demand |
| Reward earned but feature commit fails | Retain reward evidence and use idempotent feature retry |

The imported reference used a 30-second general wait and a 3-second single-use wait. These remain
examples for understanding the mechanism, not default timeout or frequency policy for a new app.
Select timeouts and caps explicitly. The SDK owns ad lifecycle; the feature owns whether a skipped
ad still permits its business continuation.
