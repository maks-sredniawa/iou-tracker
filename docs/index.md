---
layout: default
title: IOU tracker
---

# IOU tracker

<!--
    The site root, published from `docs/` at maksforge.com/<repo>/. It exists for two reasons: Play's listing
    asks for a Website, and the privacy policy needs a hosted URL. Both come free from a public
    repo — no hosting, no domain, no bill.

    Write it for someone who found the app in the store and wants to know what it is before
    installing. It is not the store listing (that is `store-listing.md`, which scripts parse) and it
    is not the README (that is for someone reading the code).
-->

**Keeps track of money you lent or borrowed, so it gets paid back.** 100 zł lent to a friend,
or money taken from the savings you share with someone: write it down once, and the app remembers
who owes whom. Free, no ads, and entirely on your own phone: no account, no sign-up, no server.

IOU tracker is still being built, and is not on Google Play yet.

## What it does

- **Both directions.** Money you lent and money you borrowed, from a person or from a shared pot,
  with what each person owes you, or you them, in one number.
- **Part of it paid back.** Record a repayment of any size; the rest stays open until it is settled,
  and settled loans stay in the history.
- **A reminder on the day.** Give a loan an optional due date, and the app reminds you on that day
  while it is not settled.

## What it does not do

IOU tracker is a notebook, not a bank. It does not move money, does not message the other person,
and does not charge interest or convert currencies. If you lose your phone without a backup, the
records go with it.

## Your data stays on your device

There is no backend to send anything to. Everything lives in the app's private storage on your phone,
and backups go to storage you choose. The details are in the

**[Privacy Policy](privacy-policy.md)**.

## Support

**iou-tracker.support@maksforge.com** — an ordinary mailbox that a person reads.

## The source

Built in the open at **[maks-sredniawa/iou-tracker](https://github.com/maks-sredniawa/iou-tracker)**.
