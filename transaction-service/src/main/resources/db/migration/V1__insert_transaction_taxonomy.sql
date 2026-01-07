-- AUTO-GENERATED FLYWAY MIGRATION FOR TRANSACTION TAXONOMY

-- PRIMARY CATEGORIES

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '89faf98d-1bca-453f-b4f0-4763b0846b80', 'INCOME', 'Income', 'https://plaid-category-icons.plaid.com/PFC_INCOME.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'INCOME'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '7914fcc2-d65c-4c17-abbe-1a019dd91b71', 'TRANSFER_IN', 'Transfer In', 'https://plaid-category-icons.plaid.com/PFC_TRANSFER_IN.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'TRANSFER_IN'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '54ce3a6e-47d7-4ffc-9726-5f5a367773c5', 'TRANSFER_OUT', 'Transfer Out', 'https://plaid-category-icons.plaid.com/PFC_TRANSFER_OUT.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'TRANSFER_OUT'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '8f780f34-6072-400a-9094-f961260b1b28', 'LOAN_PAYMENTS', 'Loan Payments', 'https://plaid-category-icons.plaid.com/PFC_LOAN_PAYMENTS.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'LOAN_PAYMENTS'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '0180fcbb-6336-430f-9e68-ba368e4cf0cb', 'BANK_FEES', 'Bank Fees', 'https://plaid-category-icons.plaid.com/PFC_BANK_FEES.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'BANK_FEES'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '38b6a541-ef2d-471c-b5f2-5cb75608949b', 'ENTERTAINMENT', 'Entertainment', 'https://plaid-category-icons.plaid.com/PFC_ENTERTAINMENT.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'ENTERTAINMENT'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b', 'FOOD_AND_DRINK', 'Food And Drink', 'https://plaid-category-icons.plaid.com/PFC_FOOD_AND_DRINK.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'FOOD_AND_DRINK'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '18b4d421-2911-44b3-832f-44b1efdb06c0', 'GENERAL_MERCHANDISE', 'General Merchandise', 'https://plaid-category-icons.plaid.com/PFC_GENERAL_MERCHANDISE.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'GENERAL_MERCHANDISE'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '632ba755-34d0-4786-b71a-2e283cdd251c', 'HOME_IMPROVEMENT', 'Home Improvement', 'https://plaid-category-icons.plaid.com/PFC_HOME_IMPROVEMENT.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'HOME_IMPROVEMENT'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '660425dd-2186-478f-92c4-df50a90cd03f', 'MEDICAL', 'Medical', 'https://plaid-category-icons.plaid.com/PFC_MEDICAL.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'MEDICAL'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT 'd7ae2c49-c264-46a4-b9ff-52b646c51914', 'PERSONAL_CARE', 'Personal Care', 'https://plaid-category-icons.plaid.com/PFC_PERSONAL_CARE.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'PERSONAL_CARE'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT 'bbe43ba6-9426-4b02-90c8-34d4695b606c', 'GENERAL_SERVICES', 'General Services', 'https://plaid-category-icons.plaid.com/PFC_GENERAL_SERVICES.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'GENERAL_SERVICES'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '75a5d55a-8c55-42f0-9517-0b1fb8e52192', 'GOVERNMENT_AND_NON_PROFIT', 'Government And Non Profit', 'https://plaid-category-icons.plaid.com/PFC_GOVERNMENT_AND_NON_PROFIT.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'GOVERNMENT_AND_NON_PROFIT'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c', 'TRANSPORTATION', 'Transportation', 'https://plaid-category-icons.plaid.com/PFC_TRANSPORTATION.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'TRANSPORTATION'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '2252a898-c7bb-499c-a976-70b862c7b948', 'TRAVEL', 'Travel', 'https://plaid-category-icons.plaid.com/PFC_TRAVEL.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'TRAVEL'
);

INSERT INTO transaction_primary_category 
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT '6554ddee-55a8-4629-acb2-e23046be2b73', 'RENT_AND_UTILITIES', 'Rent And Utilities', 'https://plaid-category-icons.plaid.com/PFC_RENT_AND_UTILITIES.png'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_primary_category WHERE category_code = 'RENT_AND_UTILITIES'
);

INSERT INTO transaction_primary_category
(transaction_primary_category_id, category_code, display_name, icon_url)
SELECT
    '3f8c2e3a-7a6f-4a4d-9c3e-9b8f4b2a1d6c',
    'OTHER',
    'Other',
    'https://plaid-category-icons.plaid.com/PFC_INCOME.png'
WHERE NOT EXISTS (
    SELECT 1
    FROM transaction_primary_category
    WHERE category_code = 'OTHER'
);

-- DETAILED CATEGORIES

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '1264bdbd-c56c-43ee-8f8d-ea69aaa92fef', 'INCOME_DIVIDENDS', 'Dividends', 'Dividends from investment accounts', '89faf98d-1bca-453f-b4f0-4763b0846b80'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'INCOME_DIVIDENDS'
);

INSERT INTO transaction_detailed_category
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT
    'a91e6c54-2d3b-4f6a-8c7e-1f2b9a4d5e30',
    'OTHER_OTHER',
    'Other',
    'Default category for unknown categories',
    '3f8c2e3a-7a6f-4a4d-9c3e-9b8f4b2a1d6c'
WHERE NOT EXISTS (
    SELECT 1
    FROM transaction_detailed_category
    WHERE category_code = 'OTHER_OTHER'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '983cf8e4-e74f-460c-b517-67d77e0c5b52', 'INCOME_INTEREST_EARNED', 'Interest Earned', 'Income from interest on savings accounts', '89faf98d-1bca-453f-b4f0-4763b0846b80'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'INCOME_INTEREST_EARNED'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '705cda57-2d86-433a-8b78-cc810b7fa379', 'INCOME_RETIREMENT_PENSION', 'Retirement Pension', 'Income from pension payments', '89faf98d-1bca-453f-b4f0-4763b0846b80'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'INCOME_RETIREMENT_PENSION'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'bc92752b-0fea-45dc-ab37-f9d9ae0d9329', 'INCOME_TAX_REFUND', 'Tax Refund', 'Income from tax refunds', '89faf98d-1bca-453f-b4f0-4763b0846b80'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'INCOME_TAX_REFUND'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'c66c93b1-b73f-4fc5-9ef5-cb9e8ff66021', 'INCOME_UNEMPLOYMENT', 'Unemployment', 'Income from unemployment benefits, including unemployment insurance and healthcare', '89faf98d-1bca-453f-b4f0-4763b0846b80'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'INCOME_UNEMPLOYMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '9ccf894f-bfca-4772-9b4a-5e7fde5b6358', 'INCOME_WAGES', 'Wages', 'Income from salaries, gig-economy work, and tips earned', '89faf98d-1bca-453f-b4f0-4763b0846b80'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'INCOME_WAGES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '6c4e22d1-293f-4055-9116-64e7b23e5e3d', 'INCOME_OTHER_INCOME', 'Other Income', 'Other miscellaneous income, including alimony, social security, child support, and rental', '89faf98d-1bca-453f-b4f0-4763b0846b80'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'INCOME_OTHER_INCOME'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '21c5d94d-ed86-4893-8347-1f69902f794e', 'TRANSFER_IN_CASH_ADVANCES_AND_LOANS', 'Cash Advances And Loans', 'Loans and cash advances deposited into a bank account', '7914fcc2-d65c-4c17-abbe-1a019dd91b71'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_IN_CASH_ADVANCES_AND_LOANS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '089af838-3c4c-4033-8339-72bed3641a15', 'TRANSFER_IN_DEPOSIT', 'Deposit', 'Cash, checks, and ATM deposits into a bank account', '7914fcc2-d65c-4c17-abbe-1a019dd91b71'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_IN_DEPOSIT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '4683e6d5-9363-43dd-ae37-db3e3910e31e', 'TRANSFER_IN_INVESTMENT_AND_RETIREMENT_FUNDS', 'Investment And Retirement Funds', 'Inbound transfers to an investment or retirement account', '7914fcc2-d65c-4c17-abbe-1a019dd91b71'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_IN_INVESTMENT_AND_RETIREMENT_FUNDS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '6d6e9f49-677d-4f98-95b4-5180f4207617', 'TRANSFER_IN_SAVINGS', 'Savings', 'Inbound transfers to a savings account', '7914fcc2-d65c-4c17-abbe-1a019dd91b71'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_IN_SAVINGS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '1c265f2e-3ea6-489c-8162-9d1077299355', 'TRANSFER_IN_ACCOUNT_TRANSFER', 'Account Transfer', 'General inbound transfers from another account', '7914fcc2-d65c-4c17-abbe-1a019dd91b71'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_IN_ACCOUNT_TRANSFER'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '07ba718f-0cc4-4fba-b2a7-be60fc8bdcac', 'TRANSFER_IN_OTHER_TRANSFER_IN', 'Other Transfer In', 'Other miscellaneous inbound transactions', '7914fcc2-d65c-4c17-abbe-1a019dd91b71'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_IN_OTHER_TRANSFER_IN'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'e8d66d4f-d3c6-4f0c-bde2-aa104bade581', 'TRANSFER_OUT_INVESTMENT_AND_RETIREMENT_FUNDS', 'Investment And Retirement Funds', 'Transfers to an investment or retirement account, including investment apps such as Acorns, Betterment', '54ce3a6e-47d7-4ffc-9726-5f5a367773c5'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_OUT_INVESTMENT_AND_RETIREMENT_FUNDS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '9abeb71a-e888-41a1-9d35-9ba4faf0a89c', 'TRANSFER_OUT_SAVINGS', 'Savings', 'Outbound transfers to savings accounts', '54ce3a6e-47d7-4ffc-9726-5f5a367773c5'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_OUT_SAVINGS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'bf01b404-224c-41f2-a582-48482e8adae5', 'TRANSFER_OUT_WITHDRAWAL', 'Withdrawal', 'Withdrawals from a bank account', '54ce3a6e-47d7-4ffc-9726-5f5a367773c5'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_OUT_WITHDRAWAL'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '33b1f22c-d89c-471d-a937-54f433fcb39d', 'TRANSFER_OUT_ACCOUNT_TRANSFER', 'Account Transfer', 'General outbound transfers to another account', '54ce3a6e-47d7-4ffc-9726-5f5a367773c5'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_OUT_ACCOUNT_TRANSFER'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'e3761efe-995e-434a-b8cc-e6c8993d89ab', 'TRANSFER_OUT_OTHER_TRANSFER_OUT', 'Other Transfer Out', 'Other miscellaneous outbound transactions', '54ce3a6e-47d7-4ffc-9726-5f5a367773c5'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSFER_OUT_OTHER_TRANSFER_OUT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '2493328c-a070-488e-b536-2ffabb054a18', 'LOAN_PAYMENTS_CAR_PAYMENT', 'Car Payment', 'Car loans and leases', '8f780f34-6072-400a-9094-f961260b1b28'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'LOAN_PAYMENTS_CAR_PAYMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'bc6a4a5d-8428-4918-bc97-38dc1abe3c84', 'LOAN_PAYMENTS_CREDIT_CARD_PAYMENT', 'Credit Card Payment', 'Payments to a credit card. These are positive amounts for credit card subtypes and negative for depository subtypes', '8f780f34-6072-400a-9094-f961260b1b28'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'LOAN_PAYMENTS_CREDIT_CARD_PAYMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'e9f59547-a100-4bd9-93ff-d20a72886868', 'LOAN_PAYMENTS_PERSONAL_LOAN_PAYMENT', 'Personal Loan Payment', 'Personal loans, including cash advances and buy now pay later repayments', '8f780f34-6072-400a-9094-f961260b1b28'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'LOAN_PAYMENTS_PERSONAL_LOAN_PAYMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '83408cc7-eeb4-49b4-851c-07f34415cf66', 'LOAN_PAYMENTS_MORTGAGE_PAYMENT', 'Mortgage Payment', 'Payments on mortgages', '8f780f34-6072-400a-9094-f961260b1b28'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'LOAN_PAYMENTS_MORTGAGE_PAYMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '3d1e552e-c5a7-41a5-b066-c00639686e93', 'LOAN_PAYMENTS_STUDENT_LOAN_PAYMENT', 'Student Loan Payment', 'Payments on student loans. For college tuition, refer to "General Services - Education"', '8f780f34-6072-400a-9094-f961260b1b28'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'LOAN_PAYMENTS_STUDENT_LOAN_PAYMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '8f947068-b755-4eb1-800a-61b241726f56', 'LOAN_PAYMENTS_OTHER_PAYMENT', 'Other Payment', 'Other miscellaneous debt payments', '8f780f34-6072-400a-9094-f961260b1b28'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'LOAN_PAYMENTS_OTHER_PAYMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '855a33c9-1cca-4de8-b60c-d3193d10981d', 'BANK_FEES_ATM_FEES', 'Atm Fees', 'Fees incurred for out-of-network ATMs', '0180fcbb-6336-430f-9e68-ba368e4cf0cb'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'BANK_FEES_ATM_FEES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '889e4258-b3b0-4948-ab05-6071290d79e0', 'BANK_FEES_FOREIGN_TRANSACTION_FEES', 'Foreign Transaction Fees', 'Fees incurred on non-domestic transactions', '0180fcbb-6336-430f-9e68-ba368e4cf0cb'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'BANK_FEES_FOREIGN_TRANSACTION_FEES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'aa9ee7d2-1dfc-4b22-915d-861dbf01f16d', 'BANK_FEES_INSUFFICIENT_FUNDS', 'Insufficient Funds', 'Fees relating to insufficient funds', '0180fcbb-6336-430f-9e68-ba368e4cf0cb'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'BANK_FEES_INSUFFICIENT_FUNDS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'd1f71a5a-6493-4235-ba1d-651be3dcbe3c', 'BANK_FEES_INTEREST_CHARGE', 'Interest Charge', 'Fees incurred for interest on purchases, including not-paid-in-full or interest on cash advances', '0180fcbb-6336-430f-9e68-ba368e4cf0cb'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'BANK_FEES_INTEREST_CHARGE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '1a620102-6c8b-460c-88c8-6a86da424d8e', 'BANK_FEES_OVERDRAFT_FEES', 'Overdraft Fees', 'Fees incurred when an account is in overdraft', '0180fcbb-6336-430f-9e68-ba368e4cf0cb'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'BANK_FEES_OVERDRAFT_FEES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '9b226857-36c1-4a19-99ef-b3a8e60facc3', 'BANK_FEES_OTHER_BANK_FEES', 'Other Bank Fees', 'Other miscellaneous bank fees', '0180fcbb-6336-430f-9e68-ba368e4cf0cb'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'BANK_FEES_OTHER_BANK_FEES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'aa6d0c5a-49ee-42a9-b4db-c2de022faac4', 'ENTERTAINMENT_CASINOS_AND_GAMBLING', 'Casinos And Gambling', 'Gambling, casinos, and sports betting', '38b6a541-ef2d-471c-b5f2-5cb75608949b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'ENTERTAINMENT_CASINOS_AND_GAMBLING'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'eea3dc4f-93dd-4072-b3d2-24eb67a10ef4', 'ENTERTAINMENT_MUSIC_AND_AUDIO', 'Music And Audio', 'Digital and in-person music purchases, including music streaming services', '38b6a541-ef2d-471c-b5f2-5cb75608949b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'ENTERTAINMENT_MUSIC_AND_AUDIO'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '73f2693f-3eb3-46ba-828a-158dd8577908', 'ENTERTAINMENT_SPORTING_EVENTS_AMUSEMENT_PARKS_AND_MUSEUMS', 'Sporting Events Amusement Parks And Museums', 'Purchases made at sporting events, music venues, concerts, museums, and amusement parks', '38b6a541-ef2d-471c-b5f2-5cb75608949b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'ENTERTAINMENT_SPORTING_EVENTS_AMUSEMENT_PARKS_AND_MUSEUMS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '8a9efc8b-e791-4e42-b430-2ad37d2f2a18', 'ENTERTAINMENT_TV_AND_MOVIES', 'Tv And Movies', 'In home movie streaming services and movie theaters', '38b6a541-ef2d-471c-b5f2-5cb75608949b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'ENTERTAINMENT_TV_AND_MOVIES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '7660e0db-5aa2-4a22-af8d-295a5c11fb84', 'ENTERTAINMENT_VIDEO_GAMES', 'Video Games', 'Digital and in-person video game purchases', '38b6a541-ef2d-471c-b5f2-5cb75608949b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'ENTERTAINMENT_VIDEO_GAMES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '0295261a-601c-4f03-9409-167e9ba9890e', 'ENTERTAINMENT_OTHER_ENTERTAINMENT', 'Other Entertainment', 'Other miscellaneous entertainment purchases, including night life and adult entertainment', '38b6a541-ef2d-471c-b5f2-5cb75608949b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'ENTERTAINMENT_OTHER_ENTERTAINMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '5db57907-664b-4f93-af9e-54f87bb94595', 'FOOD_AND_DRINK_BEER_WINE_AND_LIQUOR', 'Beer Wine And Liquor', 'Beer, Wine & Liquor Stores', 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'FOOD_AND_DRINK_BEER_WINE_AND_LIQUOR'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'cf8d5840-88ac-4ae7-8fbe-c1b18135f12d', 'FOOD_AND_DRINK_COFFEE', 'Coffee', 'Purchases at coffee shops or cafes', 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'FOOD_AND_DRINK_COFFEE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'af7da4e6-d51c-4dae-ba31-8d2f60f08ec3', 'FOOD_AND_DRINK_FAST_FOOD', 'Fast Food', 'Dining expenses for fast food chains', 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'FOOD_AND_DRINK_FAST_FOOD'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '580488e9-70c0-4d48-9467-d9e4c2f8a710', 'FOOD_AND_DRINK_GROCERIES', 'Groceries', 'Purchases for fresh produce and groceries, including farmers'' markets', 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'FOOD_AND_DRINK_GROCERIES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'bfdd927f-3241-468f-aec5-4140609bb15f', 'FOOD_AND_DRINK_RESTAURANT', 'Restaurant', 'Dining expenses for restaurants, bars, gastropubs, and diners', 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'FOOD_AND_DRINK_RESTAURANT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '656a178b-c5db-4ac9-bb97-34850a7527a5', 'FOOD_AND_DRINK_VENDING_MACHINES', 'Vending Machines', 'Purchases made at vending machine operators', 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'FOOD_AND_DRINK_VENDING_MACHINES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '5c41b107-28c4-4ad3-8467-ae9cdc06091b', 'FOOD_AND_DRINK_OTHER_FOOD_AND_DRINK', 'Other Food And Drink', 'Other miscellaneous food and drink, including desserts, juice bars, and delis', 'd2ac5c72-e47a-4eb6-82f1-6552c5101e9b'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'FOOD_AND_DRINK_OTHER_FOOD_AND_DRINK'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '159eface-989b-42c3-936a-7ded3b17a85c', 'GENERAL_MERCHANDISE_BOOKSTORES_AND_NEWSSTANDS', 'Bookstores And Newsstands', 'Books, magazines, and news', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_BOOKSTORES_AND_NEWSSTANDS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '020d10fc-639f-4ba1-8caa-8cd26b9367d9', 'GENERAL_MERCHANDISE_CLOTHING_AND_ACCESSORIES', 'Clothing And Accessories', 'Apparel, shoes, and jewelry', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_CLOTHING_AND_ACCESSORIES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'e3cee22b-2f55-48c4-b01e-61f243287fc1', 'GENERAL_MERCHANDISE_CONVENIENCE_STORES', 'Convenience Stores', 'Purchases at convenience stores', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_CONVENIENCE_STORES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '14d5094a-97c0-4e01-8f7e-613d9c5bfe3c', 'GENERAL_MERCHANDISE_DEPARTMENT_STORES', 'Department Stores', 'Retail stores with wide ranges of consumer goods, typically specializing in clothing and home goods', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_DEPARTMENT_STORES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'a3d0d26b-039c-48e9-9035-2fc22c5df02e', 'GENERAL_MERCHANDISE_DISCOUNT_STORES', 'Discount Stores', 'Stores selling goods at a discounted price', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_DISCOUNT_STORES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '6da7b0c5-95f7-4eca-915d-f8eef403bb30', 'GENERAL_MERCHANDISE_ELECTRONICS', 'Electronics', 'Electronics stores and websites', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_ELECTRONICS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'e056040e-6585-4e78-b742-eaa09ea04a20', 'GENERAL_MERCHANDISE_GIFTS_AND_NOVELTIES', 'Gifts And Novelties', 'Photo, gifts, cards, and floral stores', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_GIFTS_AND_NOVELTIES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'cadcadfc-562e-4175-90b6-2d1e1f09637f', 'GENERAL_MERCHANDISE_OFFICE_SUPPLIES', 'Office Supplies', 'Stores that specialize in office goods', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_OFFICE_SUPPLIES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '67c6fc48-d34b-4e81-83df-0747344e005d', 'GENERAL_MERCHANDISE_ONLINE_MARKETPLACES', 'Online Marketplaces', 'Multi-purpose e-commerce platforms such as Etsy, Ebay and Amazon', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_ONLINE_MARKETPLACES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '553c94e4-7aff-4892-ac40-21fb4d445e21', 'GENERAL_MERCHANDISE_PET_SUPPLIES', 'Pet Supplies', 'Pet supplies and pet food', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_PET_SUPPLIES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'da24ab47-2b2e-4aec-a932-d2018d4edc78', 'GENERAL_MERCHANDISE_SPORTING_GOODS', 'Sporting Goods', 'Sporting goods, camping gear, and outdoor equipment', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_SPORTING_GOODS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '8f6212ab-3633-4a00-bcc1-e2bb38ede2b9', 'GENERAL_MERCHANDISE_SUPERSTORES', 'Superstores', 'Superstores such as Target and Walmart, selling both groceries and general merchandise', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_SUPERSTORES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'e1635de4-81be-4acb-92b9-d7d7a434a419', 'GENERAL_MERCHANDISE_TOBACCO_AND_VAPE', 'Tobacco And Vape', 'Purchases for tobacco and vaping products', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_TOBACCO_AND_VAPE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '0e8c89d3-9647-4fcf-b374-e17c31cc648d', 'GENERAL_MERCHANDISE_OTHER_GENERAL_MERCHANDISE', 'Other General Merchandise', 'Other miscellaneous merchandise, including toys, hobbies, and arts and crafts', '18b4d421-2911-44b3-832f-44b1efdb06c0'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_MERCHANDISE_OTHER_GENERAL_MERCHANDISE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'b5ed9884-d545-46bd-9d69-c8b5620ea1cb', 'HOME_IMPROVEMENT_FURNITURE', 'Furniture', 'Furniture, bedding, and home accessories', '632ba755-34d0-4786-b71a-2e283cdd251c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'HOME_IMPROVEMENT_FURNITURE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '46507ea2-dc15-47b9-bfb0-99f3d09598a0', 'HOME_IMPROVEMENT_HARDWARE', 'Hardware', 'Building materials, hardware stores, paint, and wallpaper', '632ba755-34d0-4786-b71a-2e283cdd251c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'HOME_IMPROVEMENT_HARDWARE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '8effa36a-32c4-4863-b92f-ccd7e92f7f69', 'HOME_IMPROVEMENT_REPAIR_AND_MAINTENANCE', 'Repair And Maintenance', 'Plumbing, lighting, gardening, and roofing', '632ba755-34d0-4786-b71a-2e283cdd251c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'HOME_IMPROVEMENT_REPAIR_AND_MAINTENANCE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '57ad0f59-2238-4a23-9d0d-f13adb9826d5', 'HOME_IMPROVEMENT_SECURITY', 'Security', 'Home security system purchases', '632ba755-34d0-4786-b71a-2e283cdd251c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'HOME_IMPROVEMENT_SECURITY'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'a93bfa63-6bce-4c8e-8b2f-559715192e54', 'HOME_IMPROVEMENT_OTHER_HOME_IMPROVEMENT', 'Other Home Improvement', 'Other miscellaneous home purchases, including pool installation and pest control', '632ba755-34d0-4786-b71a-2e283cdd251c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'HOME_IMPROVEMENT_OTHER_HOME_IMPROVEMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '0d2ca43d-4d19-4bbd-8e6c-c0bf36c5b3e6', 'MEDICAL_DENTAL_CARE', 'Dental Care', 'Dentists and general dental care', '660425dd-2186-478f-92c4-df50a90cd03f'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'MEDICAL_DENTAL_CARE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '8f50481a-5197-4dcb-a44f-53fd0d470780', 'MEDICAL_EYE_CARE', 'Eye Care', 'Optometrists, contacts, and glasses stores', '660425dd-2186-478f-92c4-df50a90cd03f'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'MEDICAL_EYE_CARE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'a984ae2a-6c48-4b5c-8943-6eb808fce969', 'MEDICAL_NURSING_CARE', 'Nursing Care', 'Nursing care and facilities', '660425dd-2186-478f-92c4-df50a90cd03f'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'MEDICAL_NURSING_CARE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'd1d820f8-7427-44af-b1a7-2a1d79b4bbe7', 'MEDICAL_PHARMACIES_AND_SUPPLEMENTS', 'Pharmacies And Supplements', 'Pharmacies and nutrition shops', '660425dd-2186-478f-92c4-df50a90cd03f'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'MEDICAL_PHARMACIES_AND_SUPPLEMENTS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '90f5f2b8-2971-4ae3-b077-40070f5641e7', 'MEDICAL_PRIMARY_CARE', 'Primary Care', 'Doctors and physicians', '660425dd-2186-478f-92c4-df50a90cd03f'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'MEDICAL_PRIMARY_CARE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'a4840763-3738-4d1e-9482-324a13ebc7f9', 'MEDICAL_VETERINARY_SERVICES', 'Veterinary Services', 'Prevention and care procedures for animals', '660425dd-2186-478f-92c4-df50a90cd03f'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'MEDICAL_VETERINARY_SERVICES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '21f11ac0-2ac4-4175-8f9c-54be1a49f0ce', 'MEDICAL_OTHER_MEDICAL', 'Other Medical', 'Other miscellaneous medical, including blood work, hospitals, and ambulances', '660425dd-2186-478f-92c4-df50a90cd03f'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'MEDICAL_OTHER_MEDICAL'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '89a449a7-11f0-4c16-8fd9-f089378864c0', 'PERSONAL_CARE_GYMS_AND_FITNESS_CENTERS', 'Gyms And Fitness Centers', 'Gyms, fitness centers, and workout classes', 'd7ae2c49-c264-46a4-b9ff-52b646c51914'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'PERSONAL_CARE_GYMS_AND_FITNESS_CENTERS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '4787437d-4896-4527-8005-7261c320c8ff', 'PERSONAL_CARE_HAIR_AND_BEAUTY', 'Hair And Beauty', 'Manicures, haircuts, waxing, spa/massages, and bath and beauty products', 'd7ae2c49-c264-46a4-b9ff-52b646c51914'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'PERSONAL_CARE_HAIR_AND_BEAUTY'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '25775435-e45b-4e9f-8063-49ea7235cf7b', 'PERSONAL_CARE_LAUNDRY_AND_DRY_CLEANING', 'Laundry And Dry Cleaning', 'Wash and fold, and dry cleaning expenses', 'd7ae2c49-c264-46a4-b9ff-52b646c51914'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'PERSONAL_CARE_LAUNDRY_AND_DRY_CLEANING'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '34a0200c-a905-4b19-8f4a-f306283e28f8', 'PERSONAL_CARE_OTHER_PERSONAL_CARE', 'Other Personal Care', 'Other miscellaneous personal care, including mental health apps and services', 'd7ae2c49-c264-46a4-b9ff-52b646c51914'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'PERSONAL_CARE_OTHER_PERSONAL_CARE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '4c6c2744-124e-4c03-afc7-28ef7117bc15', 'GENERAL_SERVICES_ACCOUNTING_AND_FINANCIAL_PLANNING', 'Accounting And Financial Planning', 'Financial planning, and tax and accounting services', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_ACCOUNTING_AND_FINANCIAL_PLANNING'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '60bc2d6d-4ba6-4ff6-8681-715eab605cad', 'GENERAL_SERVICES_AUTOMOTIVE', 'Automotive', 'Oil changes, car washes, repairs, and towing', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_AUTOMOTIVE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'c5725f25-e580-442d-8ab8-5c871dde0a34', 'GENERAL_SERVICES_CHILDCARE', 'Childcare', 'Babysitters and daycare', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_CHILDCARE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '80225cfb-53d5-4466-8e68-ac503b9e1001', 'GENERAL_SERVICES_CONSULTING_AND_LEGAL', 'Consulting And Legal', 'Consulting and legal services', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_CONSULTING_AND_LEGAL'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '6dea982d-2019-4d48-863c-7bab7ea6f8a3', 'GENERAL_SERVICES_EDUCATION', 'Education', 'Elementary, high school, professional schools, and college tuition', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_EDUCATION'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'f93503da-24a9-4550-bce6-9d0d4060955a', 'GENERAL_SERVICES_INSURANCE', 'Insurance', 'Insurance for auto, home, and healthcare', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_INSURANCE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '541cedc8-b399-4fd5-a02c-0058deedcb8b', 'GENERAL_SERVICES_POSTAGE_AND_SHIPPING', 'Postage And Shipping', 'Mail, packaging, and shipping services', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_POSTAGE_AND_SHIPPING'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'f7cf3aa2-58c9-4c4d-b897-e1f3390775bd', 'GENERAL_SERVICES_STORAGE', 'Storage', 'Storage services and facilities', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_STORAGE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'e86d1e16-d030-4a49-adc6-07b43982307d', 'GENERAL_SERVICES_OTHER_GENERAL_SERVICES', 'Other General Services', 'Other miscellaneous services, including advertising and cloud storage', 'bbe43ba6-9426-4b02-90c8-34d4695b606c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GENERAL_SERVICES_OTHER_GENERAL_SERVICES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '7176e2e3-a14a-4a49-b771-533319b6efd5', 'GOVERNMENT_AND_NON_PROFIT_DONATIONS', 'Donations', 'Charitable, political, and religious donations', '75a5d55a-8c55-42f0-9517-0b1fb8e52192'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GOVERNMENT_AND_NON_PROFIT_DONATIONS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '272e9100-9508-47d0-9dbf-30868c2f1a3a', 'GOVERNMENT_AND_NON_PROFIT_GOVERNMENT_DEPARTMENTS_AND_AGENCIES', 'Government Departments And Agencies', 'Government departments and agencies, such as driving licences, and passport renewal', '75a5d55a-8c55-42f0-9517-0b1fb8e52192'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GOVERNMENT_AND_NON_PROFIT_GOVERNMENT_DEPARTMENTS_AND_AGENCIES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '8676f71d-dce1-4d1f-9278-a1a769873424', 'GOVERNMENT_AND_NON_PROFIT_TAX_PAYMENT', 'Tax Payment', 'Tax payments, including income and property taxes', '75a5d55a-8c55-42f0-9517-0b1fb8e52192'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GOVERNMENT_AND_NON_PROFIT_TAX_PAYMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '84dcf118-03ca-400e-8848-ccabf5f0b708', 'GOVERNMENT_AND_NON_PROFIT_OTHER_GOVERNMENT_AND_NON_PROFIT', 'Other Government And Non Profit', 'Other miscellaneous government and non-profit agencies', '75a5d55a-8c55-42f0-9517-0b1fb8e52192'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'GOVERNMENT_AND_NON_PROFIT_OTHER_GOVERNMENT_AND_NON_PROFIT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '9f2347a6-f4ab-46cf-9217-d4707a4ee53d', 'TRANSPORTATION_BIKES_AND_SCOOTERS', 'Bikes And Scooters', 'Bike and scooter rentals', 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSPORTATION_BIKES_AND_SCOOTERS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '6bcac3b1-40a7-487a-a92d-5da8389e2f02', 'TRANSPORTATION_GAS', 'Gas', 'Purchases at a gas station', 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSPORTATION_GAS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '8063cb06-ddc5-431c-a058-626dff1b5f99', 'TRANSPORTATION_PARKING', 'Parking', 'Parking fees and expenses', 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSPORTATION_PARKING'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '41fbce40-418e-46d0-9c80-0ae4060087fa', 'TRANSPORTATION_PUBLIC_TRANSIT', 'Public Transit', 'Public transportation, including rail and train, buses, and metro', 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSPORTATION_PUBLIC_TRANSIT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'a0a3ab02-8bb3-4baf-9140-ad0852936e03', 'TRANSPORTATION_TAXIS_AND_RIDE_SHARES', 'Taxis And Ride Shares', 'Taxi and ride share services', 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSPORTATION_TAXIS_AND_RIDE_SHARES'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'a45afffd-9053-4a09-8e99-df8e2cb6b440', 'TRANSPORTATION_TOLLS', 'Tolls', 'Toll expenses', 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSPORTATION_TOLLS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '8ef88406-8734-47bf-8b48-da8eb54f9f60', 'TRANSPORTATION_OTHER_TRANSPORTATION', 'Other Transportation', 'Other miscellaneous transportation expenses', 'c5fa2cce-89ae-41ec-bcdd-4d1badbea25c'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRANSPORTATION_OTHER_TRANSPORTATION'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '26a6214e-a7d0-4f94-9259-8ea1cf32ec8d', 'TRAVEL_FLIGHTS', 'Flights', 'Airline expenses', '2252a898-c7bb-499c-a976-70b862c7b948'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRAVEL_FLIGHTS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '7a1d2d70-7630-47c3-a077-6cbcde0dd4ac', 'TRAVEL_LODGING', 'Lodging', 'Hotels, motels, and hosted accommodation such as Airbnb', '2252a898-c7bb-499c-a976-70b862c7b948'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRAVEL_LODGING'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '16703e07-3dc2-4ec1-8018-b3edc26b7619', 'TRAVEL_RENTAL_CARS', 'Rental Cars', 'Rental cars, charter buses, and trucks', '2252a898-c7bb-499c-a976-70b862c7b948'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRAVEL_RENTAL_CARS'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '85fe10fe-0f17-493a-928c-ba501b08193e', 'TRAVEL_OTHER_TRAVEL', 'Other Travel', 'Other miscellaneous travel expenses', '2252a898-c7bb-499c-a976-70b862c7b948'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'TRAVEL_OTHER_TRAVEL'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'b5785576-ce3f-4c14-896c-70f0e81176fe', 'RENT_AND_UTILITIES_GAS_AND_ELECTRICITY', 'Gas And Electricity', 'Gas and electricity bills', '6554ddee-55a8-4629-acb2-e23046be2b73'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'RENT_AND_UTILITIES_GAS_AND_ELECTRICITY'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '2dd8f16f-14dc-480c-bf0e-f10fc7e08f57', 'RENT_AND_UTILITIES_INTERNET_AND_CABLE', 'Internet And Cable', 'Internet and cable bills', '6554ddee-55a8-4629-acb2-e23046be2b73'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'RENT_AND_UTILITIES_INTERNET_AND_CABLE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT 'bfc42194-1fcf-43e7-907a-7180bdf82b25', 'RENT_AND_UTILITIES_RENT', 'Rent', 'Rent payment', '6554ddee-55a8-4629-acb2-e23046be2b73'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'RENT_AND_UTILITIES_RENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '59da5817-5fea-4134-bd6b-63e74a2bf5cf', 'RENT_AND_UTILITIES_SEWAGE_AND_WASTE_MANAGEMENT', 'Sewage And Waste Management', 'Sewage and garbage disposal bills', '6554ddee-55a8-4629-acb2-e23046be2b73'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'RENT_AND_UTILITIES_SEWAGE_AND_WASTE_MANAGEMENT'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '63cdb5d8-f3b8-4457-accf-1f446c719a8f', 'RENT_AND_UTILITIES_TELEPHONE', 'Telephone', 'Cell phone bills', '6554ddee-55a8-4629-acb2-e23046be2b73'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'RENT_AND_UTILITIES_TELEPHONE'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '591f1e16-e0be-47c4-badb-796de31ba0a6', 'RENT_AND_UTILITIES_WATER', 'Water', 'Water bills', '6554ddee-55a8-4629-acb2-e23046be2b73'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'RENT_AND_UTILITIES_WATER'
);

INSERT INTO transaction_detailed_category 
(transaction_detailed_category_id, category_code, display_name, description, primary_category_id)
SELECT '51cfff9e-dfb7-44af-bbe1-436992dc742f', 'RENT_AND_UTILITIES_OTHER_UTILITIES', 'Other Utilities', 'Other miscellaneous utility bills', '6554ddee-55a8-4629-acb2-e23046be2b73'
WHERE NOT EXISTS (
    SELECT 1 FROM transaction_detailed_category WHERE category_code = 'RENT_AND_UTILITIES_OTHER_UTILITIES'
);
