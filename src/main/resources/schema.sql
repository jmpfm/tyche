create table if not exists portfolio_positions (
	id bigint auto_increment primary key,
	symbol varchar(32) not null,
	name varchar(255) not null,
	asset_class varchar(64) not null,
	quantity decimal(28, 8) not null,
	average_cost decimal(28, 8) not null,
	market_price decimal(28, 8) not null,
	day_change_percent decimal(12, 4) not null default 0,
	display_order integer not null default 0,
	constraint uk_portfolio_positions_symbol unique (symbol)
);
