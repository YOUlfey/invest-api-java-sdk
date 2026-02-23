package ru.ttech.piapi.strategy.candle.backtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ta4j.core.Bar;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.analysis.cost.ZeroCostModel;
import org.ta4j.core.backtest.BarSeriesManager;
import org.ta4j.core.bars.TimeBarBuilderFactory;
import org.ta4j.core.num.DecimalNumFactory;
import ru.ttech.piapi.strategy.candle.mapper.BarMapper;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CandleStrategyBacktest {

  private static final Logger logger = LoggerFactory.getLogger(CandleStrategyBacktest.class);
  private final CandleStrategyBacktestConfiguration config;
  private final BarsLoader barsLoader;

  public CandleStrategyBacktest(
    CandleStrategyBacktestConfiguration config,
    BarsLoader barsLoader
  ) {
    this.config = config;
    this.barsLoader = barsLoader;
  }

  public void run() {
    logger.info("Backtest started...");
    for (String instrumentId : config.getInstrumentIds()) {
      var barsData = barsLoader.loadBars(
        instrumentId, config.getCandleInterval(), config.getFrom(), config.getTo()
      );
      List<Bar> bars = StreamSupport.stream(barsData.spliterator(), false)
        .map(barData -> BarMapper.mapBarDataWithIntervalToBar(barData, config.getCandleInterval()))
        .collect(Collectors.toList());
      BarSeries barSeries = new BaseBarSeriesBuilder()
        .withNumFactory(DecimalNumFactory.getInstance())
        .withBarBuilderFactory(new TimeBarBuilderFactory())
        .withBars(bars)
        .build();
      doSingleRun(barSeries);
    }
    logger.info("Backtest finished!");
  }

  private void doSingleRun(BarSeries series) {
    var tradeFeeModel = config.getTradeFeeModel();
    var tradeExecutionModel = config.getTradeExecutionModel();
    var barSeriesManager = new BarSeriesManager(series, tradeFeeModel, new ZeroCostModel(), tradeExecutionModel);
    config.getStrategyAnalysis().accept(barSeriesManager);
  }
}
