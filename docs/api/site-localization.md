# EN/VI presentation contract

Existing requests and objective fields are unchanged. The website language preference remains local browser UI state; it is not an identity or authorization mechanism.

`POST /api/v1/simulations/{slug}/attempts` returns the existing evaluated response. A known current outcome may additionally contain `explanationVi`, a reviewed Vietnamese translation of its existing English `explanation`. Both are post-evaluation content. Unknown explanations omit the optional field, retaining English text. The field is added by a response mapper after the service has evaluated and persisted the result; it is not an evaluator input or additional persisted score.

`GET /api/v1/simulations/{slug}/tasks` is unchanged. It includes public questions and options, never this translation field or evaluation rules. No new endpoint or database migration is added.

Syn requests reuse existing `responseLanguage: EN | VI`. AUTO in the UI resolves to the website preference before sending; an explicit Syn-only override is supported. API response validation and fallback are unchanged. Source passages and historical messages retain their original wording.

Existing demo/header identity limitations remain open and are not resolved by localization.
