# Changelog

## [0.3.1](https://github.com/maks-sredniawa/iou-tracker/compare/v0.3.0...v0.3.1) (2026-11-20)


### Bug Fixes

* correct the Currency picker where a phone's currency data is out of date ([d8f58c5](https://github.com/maks-sredniawa/iou-tracker/commit/d8f58c59f889c55d770b2c91d573e181f3f602c0))
* drop SVC from the currencies an old platform lacks ([194a1e6](https://github.com/maks-sredniawa/iou-tracker/commit/194a1e66b3c2c5d5bfbf2c5a7389e3c09330b754))
* give the Add a loan button a name TalkBack can read ([c941a11](https://github.com/maks-sredniawa/iou-tracker/commit/c941a11bf762e1264af5383ac668864e227d2155))
* stop edge-to-edge.py calling a scene it skipped flaky and reached ([2032d51](https://github.com/maks-sredniawa/iou-tracker/commit/2032d516d8832d43f1ec4ec760e39fa23d88366c))
* store a withdrawn code's successor as the first-run Currency ([47e19d3](https://github.com/maks-sredniawa/iou-tracker/commit/47e19d3e0ab824e1d4bf2dfde65dab6c5ce86d77))

## [0.3.0](https://github.com/maks-sredniawa/iou-tracker/compare/v0.2.0...v0.3.0) (2026-11-13)


### Features

* add a Loan from Home or a Counterparty, writing a new name and its Loan in one transaction ([9eeccfa](https://github.com/maks-sredniawa/iou-tracker/commit/9eeccfa7a9e605e196738ef893a0b4ff824deb9d))
* **support:** adopt the shared Help and feedback from android-starter ([705b22c](https://github.com/maks-sredniawa/iou-tracker/commit/705b22c40b139f053652917f4ea86e4e7eda416a))
* **support:** link licence terms and show licence names on Credits ([6046bb0](https://github.com/maks-sredniawa/iou-tracker/commit/6046bb070d3d9f05f1667d78dde2b258ed80bf5a))
* **support:** open Credits with the people who made the app ([87cbd5f](https://github.com/maks-sredniawa/iou-tracker/commit/87cbd5f18da3f06b5a49a127dabe78eca7a9f593))
* **support:** sync the address row and copy string, and point docs at SupportConfig ([8309ff9](https://github.com/maks-sredniawa/iou-tracker/commit/8309ff99b8c982f2d43941374cd80a6ce2a792ff))


### Bug Fixes

* leave the ISO fund codes out of the Currency picker ([2a57536](https://github.com/maks-sredniawa/iou-tracker/commit/2a57536b722c0df567071c769d192c110e671205))
* leave the X codes that are not currencies out of the Currency picker ([5968ad2](https://github.com/maks-sredniawa/iou-tracker/commit/5968ad25f6fafabe0bcd207bc9ec4c21d005955a))
* put adjustResize on MainActivity so the keyboard no longer covers Save on API 26-29 ([ff3f3c9](https://github.com/maks-sredniawa/iou-tracker/commit/ff3f3c9f1174e72a5178a26fa7e1f9c889d13dd4))
* **support:** say when the language report finds no mail app, and keep the licence screen safe ([7bbd036](https://github.com/maks-sredniawa/iou-tracker/commit/7bbd036133f59c9ab24b34c2243b3f5e7e766487))
* tint the Counterparty icon onSurfaceVariant and read the Pot chip once ([63c7cb5](https://github.com/maks-sredniawa/iou-tracker/commit/63c7cb5c69ea23631d5cf881a6d76657dae7e175))

## [0.2.0](https://github.com/maks-sredniawa/iou-tracker/compare/v0.1.0...v0.2.0) (2026-11-12)


### Features

* give IOU tracker its Tally mark and indigo palette ([#3](https://github.com/maks-sredniawa/iou-tracker/issues/3)) ([c9f3dd8](https://github.com/maks-sredniawa/iou-tracker/commit/c9f3dd8a879b79f483fa52218f8278960f537ad3))
* parse a typed Amount and plan a Currency rescale, with name matching and the picker's day ([ea30792](https://github.com/maks-sredniawa/iou-tracker/commit/ea30792a603a9bfc67f75f7b52896f6548481435))
* phase 0, schema 1 and Home, without photos or exact alarms ([#4](https://github.com/maks-sredniawa/iou-tracker/issues/4)) ([36c263a](https://github.com/maks-sredniawa/iou-tracker/commit/36c263ab053a71542db754ba8bd2331cbaa6285e))
* read a lone mark before three digits as decimal whenever its grouping reading is invalid ([0ec8b16](https://github.com/maks-sredniawa/iou-tracker/commit/0ec8b165f39ea116df3438a472b21b233ea8ed76))
* refuse an Amount as ambiguous only when both readings are valid, and accept trailing zeros ([7725611](https://github.com/maks-sredniawa/iou-tracker/commit/7725611b4eeb00148b5cd35363b109237bbad854))
* show and change the Currency in Settings, rescaling every Amount exactly or refusing the change ([43c882d](https://github.com/maks-sredniawa/iou-tracker/commit/43c882d35e12c546a56813920d1c3249301d04fb))


### Bug Fixes

* refuse a Currency change from a stored code this phone cannot resolve, instead of relabelling it ([f70c1f6](https://github.com/maks-sredniawa/iou-tracker/commit/f70c1f600c344c7e6ba76dedd7642fbf737c53b7))

## Changelog

Nothing released yet.
