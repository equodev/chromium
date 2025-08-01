# Contributing to Equo Chromium

Thank you for your interest in contributing to Equo Chromium! We want to make contributing to this project as easy and transparent as
possible.

## Issue Reporting Guidelines

* If you have a question, you can get quick answers from our [Discord chat](https://discord.gg/fFEEhm8etB).
* The issue list of this repository is exclusively for bug reports and feature requests. Non-conforming issues will be closed immediately.
* Try to search for the you want to create, it may have already been answered or even fixed in the development branch (`develop`).
* It is required that you clearly describe the steps necessary to reproduce the issue you are running into. Although we would love to help our users as much as possible, diagnosing issues without clear reproduction steps is extremely time-consuming and simply not sustainable.
* Use only the minimum amount of code necessary to reproduce the unexpected behavior. A good bug report should isolate specific methods that exhibit unexpected behavior and precisely define how expectations were violated. What did you expect the method or methods to do, and how did the observed behavior differ? The more precisely you isolate the issue, the faster we can investigate.
* Issues with no clear repro steps will not be triaged. If an issue labeled "need repro" receives no further input from the issue author for more than 5 days, it will be closed.
* Most importantly, we beg your patience: our team must balance your request against many other responsibilities — fixing other bugs, answering other questions, new features, new documentation, etc. The issue list is not paid support and we cannot make guarantees about how fast your issue can be resolved.

## Pull Request Guidelines

* If you are adding a new feature, please provide convincing reason to add this feature. Ideally you should open a suggestion issue first and have it greenlighted before working on it.
* If you are fixing a bug:
  * If you are resolving a special issue, add `(fix: #xxxx[,#xxx])` (#xxxx is the issue id) in your PR title for a better release log, e.g. `fix: update window default size (fix #2349)`.
  * Provide detailed description of the bug in the PR, or link to an issue that does.

## Development Steps

Join our [Discord server](https://discord.gg/fFEEhm8etB) and let us know that you want to contribute.

## Coding Style

You can check code style compliance inside the IDE configured in the previous section, or by running `mvn checkstyle:check` in the root of the project.

It's necessary that you comply with the project's code style for your contribution to be accepted.
