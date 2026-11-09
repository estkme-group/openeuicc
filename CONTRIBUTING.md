Contributing to OpenEUICC
---

Thank you for your interest in conrtibuting to OpenEUICC. Before you start, please read this document _in full_.

Features
---

In general, OpenEUICC welcomes contributions for bugfixes and essential features that are part of GSMA standards or AOSP platform but are currently omitted from OpenEUICC. If your intended contribution falls into this category, feel free to take a crack at it. We kindly ask you to review our LLM policy listed below before submitting your patches. In some cases, we may request that you split your patch into multiple patches to facilitate the review process.

"Nice to have", or "quality of life" features should start as a discussion in the issue list. We will likely reject unsolicited pull requests adding features that have not been discussed before.

`lpac-jni` and `lpac`
---

Contributions involving changes to `lpac` should be made there first. As a rule of thumb, OpenEUICC aims __not__ to carry additional patches on top of `lpac`. By contributing to `lpac`, you also ensure that the improvement is carried over to other platforms supported by `lpac`, not just OpenEUICC.

Translations
---

Unfortunately, OpenEUICC does not accept one-off translation pull requests, and does not make use of crowd-sourced translation platforms like Weblate or Crowdin. Any such contributions will likely be rejected.

It is in our interest to support OpenEUICC in more languages. However, based on our experience, it is very difficult to maintain quality and timely updates under the crowd-sourced translation model, especially with the currently limited resources of this project. Machine translations are not valuable additions to this project. Nor are erroneous or out-of-date translations.

In this spirit, any language supported by this project must always be kept up-to-date when a release is tagged. This means that there must be someone committed to maintaining the translations for any language included. If you are willing to sign up for this, raise an issue and we can discuss how to move forward.

LLM Policy
---

You are allowed to use an LLM to help with:

- Understanding the codebase
- Proposing resolutions your issue
- Drafting patches and tests

The following behaviors are explicitly __prohibited__:

- Submitting fully LLM-generated pull requests without understanding your code
  - If you do not have the means to review LLM code, submit an issue instead and describe what you want in your own words. You may attach the LLM-suggested patch to the issue. __Do not submit a pull request that you do not understand__.
- Posting LLM-generated text in the issue list, pull requests, commit messages, or in responses to issues or pull request reviews
  - If you find it valuable, it is okay to, for example, attach a LLM transcript for _your attempt at diagnosing the issue_. It cannot be the main bulk of your submission. You must describe what you understand of the issue and/or fix in your own words.
- Unsolicited, automated issue or pull request creation with the help of LLM tools (or really any automation in general)

Before submitting pull requests, we kindly ask you to understand that most of the maintainers also have access to the same tools as you do, and a purely LLM generated pull requests offers no value without your expertise and input. If your contribution boils down to only token costs, it probably belongs to a discussion in the issue list instead.

