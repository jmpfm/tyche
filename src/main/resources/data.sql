merge into portfolio_positions (
	symbol,
	name,
	asset_class,
	quantity,
	average_cost,
	market_price,
	day_change_percent,
	display_order
) key(symbol) values (
	'VOO',
	'Vanguard S&P 500 ETF',
	'ETF',
	18.0000,
	425.30,
	482.75,
	0.42,
	10
);

merge into portfolio_positions (
	symbol,
	name,
	asset_class,
	quantity,
	average_cost,
	market_price,
	day_change_percent,
	display_order
) key(symbol) values (
	'AAPL',
	'Apple Inc.',
	'Equity',
	22.0000,
	168.40,
	186.90,
	0.87,
	20
);

merge into portfolio_positions (
	symbol,
	name,
	asset_class,
	quantity,
	average_cost,
	market_price,
	day_change_percent,
	display_order
) key(symbol) values (
	'MSFT',
	'Microsoft Corp.',
	'Equity',
	10.0000,
	391.20,
	438.15,
	-0.18,
	30
);

merge into portfolio_positions (
	symbol,
	name,
	asset_class,
	quantity,
	average_cost,
	market_price,
	day_change_percent,
	display_order
) key(symbol) values (
	'BTC',
	'Bitcoin',
	'Crypto',
	0.1850,
	58250.00,
	64120.00,
	1.35,
	40
);

merge into portfolio_positions (
	symbol,
	name,
	asset_class,
	quantity,
	average_cost,
	market_price,
	day_change_percent,
	display_order
) key(symbol) values (
	'CASH',
	'Available cash',
	'Cash',
	1.0000,
	7350.00,
	7350.00,
	0.00,
	50
);
