package com.linkedin.pinot.core.query.aggregation.function;

import java.util.List;
import java.util.NoSuchElementException;

import org.json.JSONException;
import org.json.JSONObject;

import com.linkedin.pinot.common.data.FieldSpec.DataType;
import com.linkedin.pinot.common.request.AggregationInfo;
import com.linkedin.pinot.core.common.Block;
import com.linkedin.pinot.core.common.BlockDocIdIterator;
import com.linkedin.pinot.core.common.BlockSingleValIterator;
import com.linkedin.pinot.core.common.BlockValIterator;
import com.linkedin.pinot.core.common.Constants;
import com.linkedin.pinot.core.query.aggregation.AggregationFunction;
import com.linkedin.pinot.core.query.aggregation.CombineLevel;
import com.linkedin.pinot.core.segment.index.readers.DictionaryReader;


public class MinAggregationFunction implements AggregationFunction<Double, Double> {
  private String _minColumnName;

  public MinAggregationFunction() {

  }

  @Override
  public void init(AggregationInfo aggregationInfo) {
    _minColumnName = aggregationInfo.getAggregationParams().get("column");
  }

  @Override
  public Double aggregate(Block docIdSetBlock, Block[] block) {
    double ret = Double.POSITIVE_INFINITY;
    double tmp = 0;
    int docId = 0;
    DictionaryReader dictionaryReader = block[0].getMetadata().getDictionary();
    BlockDocIdIterator docIdIterator = docIdSetBlock.getBlockDocIdSet().iterator();
    BlockSingleValIterator blockValIterator = (BlockSingleValIterator) block[0].getBlockValueSet().iterator();

    while ((docId = docIdIterator.next()) != Constants.EOF) {
      blockValIterator.skipTo(docId);
      tmp = dictionaryReader.getDoubleValue(blockValIterator.nextIntVal());
      if (tmp < ret) {
        ret = tmp;
      }
    }
    return ret;
  }

  @Override
  public Double aggregate(Double mergedResult, int docId, Block[] block) {
    BlockSingleValIterator blockValIterator = (BlockSingleValIterator) block [0].getBlockValueSet().iterator();
    blockValIterator.skipTo(docId);
    if (mergedResult == null) {
      return block[0].getMetadata().getDictionary().getDoubleValue(blockValIterator.nextIntVal());
    }
    double tmp = block[0].getMetadata().getDictionary().getDoubleValue(blockValIterator.nextIntVal());
    if (tmp < mergedResult) {
      return tmp;
    }
    return mergedResult;
  }

  @Override
  public Double aggregate(BlockValIterator[] blockValIterators) {
    double ret = Double.POSITIVE_INFINITY;
    double tmp = 0;
    BlockSingleValIterator blockValIterator = (BlockSingleValIterator) blockValIterators[0];
    while (blockValIterator.hasNext()) {
      tmp = blockValIterator.nextDoubleVal();
      if (tmp < ret) {
        ret = tmp;
      }
    }
    return ret;
  }

  @Override
  public Double aggregate(Double oldValue, BlockValIterator[] blockValIterators) {
    BlockSingleValIterator blockValIterator = (BlockSingleValIterator) blockValIterators[0];
    if (oldValue == null) {
      return blockValIterator.nextDoubleVal();
    }
    double tmp = blockValIterator.nextDoubleVal();
    if (tmp < oldValue) {
      return tmp;
    }
    return oldValue;
  }

  @Override
  public List<Double> combine(List<Double> aggregationResultList, CombineLevel combineLevel) {
    double minValue = Double.POSITIVE_INFINITY;
    for (double aggregationResult : aggregationResultList) {
      if (aggregationResult < minValue) {
        minValue = aggregationResult;
      }
    }
    aggregationResultList.clear();
    aggregationResultList.add(minValue);
    return aggregationResultList;
  }

  @Override
  public Double combineTwoValues(Double aggregationResult0, Double aggregationResult1) {
    if (aggregationResult0 == null) {
      return aggregationResult1;
    }
    if (aggregationResult1 == null) {
      return aggregationResult0;
    }
    return (aggregationResult0 < aggregationResult1) ? aggregationResult0 : aggregationResult1;
  }

  @Override
  public Double reduce(List<Double> combinedResultList) {
    double minValue = Double.POSITIVE_INFINITY;
    for (double combinedResult : combinedResultList) {
      if (combinedResult < minValue) {
        minValue = combinedResult;
      }
    }
    return minValue;
  }

  @Override
  public JSONObject render(Double finalAggregationResult) {
    try {
      if (finalAggregationResult == null) {
        throw new NoSuchElementException("Final result is null!");
      }
      return new JSONObject().put("value", String.format("%1.5f", finalAggregationResult));
    } catch (JSONException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public DataType aggregateResultDataType() {
    return DataType.DOUBLE;
  }

  @Override
  public String getFunctionName() {
    return "min_" + _minColumnName;
  }

}