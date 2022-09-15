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

## To be determined
1. Optimal frequency and period for computations