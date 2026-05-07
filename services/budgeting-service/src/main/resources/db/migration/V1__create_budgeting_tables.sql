CREATE TABLE budgets (
    budget_id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    category_id UUID NOT NULL,
    limit_amount NUMERIC(19, 4) NOT NULL,
    spent_amount NUMERIC(19, 4) NOT NULL,
    iso_currency_code VARCHAR(3) NOT NULL,
    period VARCHAR(20) NOT NULL,
    period_start DATE NOT NULL,
    active BOOLEAN NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE processed_transactions (
    transaction_id UUID PRIMARY KEY,
    budget_id UUID NOT NULL,
    amount NUMERIC(19, 4) NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE NOT NULL
);
