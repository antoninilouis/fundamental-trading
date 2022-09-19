# fundamental-trading
## Stock selection
### Inputs
For Nasdaq-100 with ^GSPC (S&P 500) as index
1. Prices last 10y (currently 2012-01-01 ~ 2022-09-01)
2. Latest ROE (2021)
3. Latest dividend payout ratio (2021)

## Portfolio optimization
### Inputs
1. Time series of the returns of the S&P500
2. Time series of the returns of the shares
3. Time series of the risk-free rate (daily t-bills rate)
4. Number of observations
5. Alpha values
6. Beta coefficients
7. SSE (getSumSquaredErrors) or MSE (getMeanSquaredError)

### Process
1. Preliminary computations
   1. Variance of stock residuals (s^2(ei)) = SSE / N - 1 = MSE
2. Compute the optimal portfolio weights using the analytical method

## Trading

### Phases
The transition into trading will be done in 3 phases:
1. Backtesting
2. Paper trading
3. Live trading

Backtesting
- Recovery of stock pricing data from API to back SymbolStatisticsRepository
  - Use community Java SDK: https://github.com/Petersoj/alpaca-java
  - Load historical data for period/frequency
- Add support for ZonedDateTime to SymbolStatisticsRepository
- Backtest on different periods/frequencies of modelling/trading
- Expand symbols list
    - IEX eligible symbols: https://iextrading.com/trading/eligible-symbols/
- Recovery of stock fundamental data (for paper trading) from script
    - ... or Paid API (50$/m): https://eodhistoricaldata.com

Paper trading
- Create real time SymbolStatisticsRepository
- Create trading logic (safe portfolio lifecycle)
  - portfolio value tracking, safe orders, max portfolio creation delay, auto/manual mode
- Add admin and monitoring
  - panic button, approve button, logging

Live trading

### To be determined
1. Preferred frequency and period of modelling
2. Preferred frequency of trading