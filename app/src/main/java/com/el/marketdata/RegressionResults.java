package com.el.marketdata;

public class RegressionResults {

  final private Double slope;
  final private Double intercept;
  final private Double sumSquaredErrors;
  final private Double meanSquareError;

  public RegressionResults(
    Double slope,
    Double intercept,
    Double sumSquaredErrors,
    Double meanSquareError
  ) {
    this.slope = slope;
    this.intercept = intercept;
    this.sumSquaredErrors = sumSquaredErrors;
    this.meanSquareError = meanSquareError;
  }

  public Double getSlope() {
    return slope;
  }

  public Double getIntercept() {
    return intercept;
  }

  public Double getSumSquaredErrors() {
    return sumSquaredErrors;
  }

  public Double getMeanSquareError() {
    return meanSquareError;
  }
}
