# Currency Notes

## Insights Service

- Accounts and transactions keep provider/source currency as-is.
- User preferred currency should not mutate stored transaction or account data.
- Insights should avoid storing user-preferred currency amounts because preference changes would require rewriting analytics data.
- For insight reads, aggregate raw transaction amounts by source currency first, then convert those grouped totals to the user's current preferred currency.
- Example:
  - Food spending stored as `150 USD`, `20 EUR`, `500 TRY`.
  - If preferred currency is `TRY`, convert each grouped currency total to `TRY`, then sum for the displayed category total.
- This avoids converting every row one-by-one and avoids database rewrites when preferred currency changes.
- Insights can optionally return a source-currency breakdown for transparency.

## Budgeting

- Budget currency conversion is not finalized.
- We still need to discuss how existing budgets should behave when user preferred currency changes.
- Open question: should active budgets be converted to the new preferred currency, or should budget currency remain fixed after creation?
- Open question: if budgets are converted, should `spentAmount` be converted directly or recalculated from processed transaction source amounts?
