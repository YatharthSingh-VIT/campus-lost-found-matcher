# Campus Lost & Found Matcher

## Problem

A lost-item message and a matching found-item message may be posted in different places. Someone must find both, compare the details, and check whether either report has already been resolved. This project brings those records into one searchable collection.

## Objective

Help students and campus help-desk staff record lost and found items, identify possible matches, and keep report statuses up to date.

## Scope

The application runs in a Java terminal on a shared computer. It saves reports to a local CSV file and reloads them between sessions. Each report includes an item description, category, location, date, contact reference, and status.

The matching rules compare category, location, dates, and keywords. Results explain the score so an operator can decide which reports deserve follow-up. A score does not establish ownership.

## Target users

Students reporting or searching for belongings, and help-desk staff managing those reports through the same installation.

## Main features

1. Add, view, edit, delete, resolve, and reopen lost or found reports.
2. Rank eligible report pairs and display their score breakdowns.
3. Search with combined filters and review counts by type, status, and category.

## Boundaries

This version has no accounts, remote synchronization, notifications, or image recognition. Ownership checks and the physical return of an item remain manual. The demonstration uses fictional reports and desk references.
