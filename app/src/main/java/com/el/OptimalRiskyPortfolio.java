package com.el;

import com.el.marketdata.MarketDataRepository;
import com.el.stockselection.CAPM;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Math.pow;
import static java.util.Map.Entry;

public class OptimalRiskyPortfolio {

    final private MarketDataRepository marketDataRepository;
    final private Set<String> selection;

    public OptimalRiskyPortfolio(MarketDataRepository marketDataRepository, Set<String> selection) {
        this.marketDataRepository = marketDataRepository;
        this.selection = selection;
    }

    public Map<String, Double> calculate() {
        Map<String, Double> optimalAllocation = new HashMap<>();

        if (selection.size() == 0) {
            optimalAllocation.put(MarketDataRepository.INDEX_NAME, 1.0);
            return optimalAllocation;
        }

        var regressionResults = marketDataRepository.getStockRegressionResults(selection);

        // 1) Calculate the initial weight of each stock in the active portfolio
        var initialWeights = regressionResults.entrySet().stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> entry.getValue().getIntercept() / entry.getValue().getMeanSquareError()
            ));

        // 2) Scale the initial weights so they sum to 1
        var scaledWeights = initialWeights.entrySet().stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> entry.getValue() / initialWeights.values().stream().mapToDouble(Double::doubleValue).sum()
            ));

        // 3) Calculate the weighted alphas of the active portfolio
        var portfolioWeightedAlphas = scaledWeights.entrySet().stream().mapToDouble(
            entry -> entry.getValue() * regressionResults.get(entry.getKey()).getIntercept()
        ).sum();

        // 4) Calculate the residual variance of the active portfolio
        var portfolioResidualVariance = scaledWeights.entrySet().stream().mapToDouble(
            entry -> pow(entry.getValue(), 2) * regressionResults.get(entry.getKey()).getMeanSquareError()
        ).sum();

        // 5) Calculate the weighted β A of the active portfolio
        var portfolioWeightedBeta = scaledWeights.entrySet().stream().mapToDouble(
            entry -> entry.getValue() * regressionResults.get(entry.getKey()).getSlope()
        ).sum();

        // 6) Calculate the initial weight of the active portfolio
        var ds = new DescriptiveStatistics();
        marketDataRepository.getPastIndexReturns().values().forEach(ds::addValue);
        var marketVariance = ds.getVariance();
        var erm = CAPM.calculateMeanMarketReturns(marketDataRepository.getPastIndexReturns());
        var portfolioInitialWeight = (portfolioWeightedAlphas / portfolioResidualVariance) / (erm / marketVariance);

        // 7) Calculate the final weight of the active portfolio by adjusting for βA
        var portfolioFinalWeight = portfolioInitialWeight / (1.0 + (1.0 - portfolioWeightedBeta) * portfolioInitialWeight);

        // 8) Calculate the weights of the optimal risky portfolio, including the passive portfolio and each security in the active portfolio
        optimalAllocation = scaledWeights.entrySet().stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                entry -> portfolioFinalWeight * entry.getValue()
            ));

        // 2.1) Calculate the expected risk premium for the portfolio
        double marketWeight = 1.0 - portfolioFinalWeight;
        double expectedRiskPremium = portfolioFinalWeight * portfolioWeightedAlphas + (marketWeight + portfolioWeightedAlphas * portfolioWeightedBeta) * erm;

        optimalAllocation.put(MarketDataRepository.INDEX_NAME, marketWeight);
        return optimalAllocation;
    }
}
