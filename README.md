# fundamental-trading
## Stock selection
### Inputs
For Nasdaq-100 with ^GSPC (S&P 500) as index
1. Prices last 10y (currently 2012-01-01 ~ 2022-09-01)
2. Latest ROE (2021)
3. Latest dividend payout ratio (2021)
4. Dividends per share

## Trading
The transition to trading will be done in 3 phases: backtesting, paper trading, live trading

### Backtesting
- [x] Recovery of stock pricing data from API to back SymbolStatisticsRepository
  - Use community Java SDK: https://github.com/Petersoj/alpaca-java
  - Load historical data for period/frequency
- [ ] Add support for ZonedDateTime to SymbolStatisticsRepository
  - https://alpaca.markets/docs/api-references/market-data-api/stock-pricing-data/historical/
- [ ] Backtest on different periods/frequencies of modelling/trading
- [ ] Expand symbols list
  - IEX eligible symbols: https://iextrading.com/trading/eligible-symbols/
- [ ] Recovery of stock fundamental data (for paper trading) from script
  - ... or Paid API (50$/m): https://eodhistoricaldata.com

### Paper trading
- Create real time SymbolStatisticsRepository
- Create trading logic (safe portfolio lifecycle)
  - portfolio value tracking, safe orders, max portfolio creation delay, auto/manual mode
- Add admin and monitoring
  - panic button, approve button, logging

### Live trading

## Project management
### Where are we?

SymbolStatisticsRepository: fetch data from sources, do basic calculation e.g returns 
EquityScreener: select stocks based on metrics e.g CAPM
OptimalRiskyPortfolio: composes an ORP from a stock selection

### Where do we want to go?

Features to add
- support for hourly prices
- refresh stock prices RegressionResults at chosen frequency
- remote source for fundamental data
- extend list of symbols to IEX universe

Logistics
- add separate local and remote MarketDataRepository
- add unit tests

## To consider later
1. Preferred frequency and period of modelling
2. Preferred frequency of trading

--

1. Make backtest with data from FMP
2. Make backtest on market IEX