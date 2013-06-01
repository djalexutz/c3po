package com.petpet.c3po.analysis;

import junit.framework.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.junit.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.MapReduceOutput;
import com.petpet.c3po.analysis.mapreduce.HistogramJob;
import com.petpet.c3po.analysis.mapreduce.NumericAggregationJob;
import com.petpet.c3po.api.dao.PersistenceLayer;
import com.petpet.c3po.common.Constants;
import com.petpet.c3po.utils.Configurator;

public class MapReduceTest {
  private static final Logger log = LoggerFactory.getLogger(MapReduceTest.class);

  @Test
  public void shouldTestAggregationMapReduce() throws Exception {
    Configurator.getDefaultConfigurator().configure();
    
    NumericAggregationJob job = new NumericAggregationJob("test1", "size");
    MapReduceOutput output = job.execute();
    
    System.out.println(output);
    Assert.assertNotNull(output);
  }
  
  @Test
  public void shouldTestHistogramMapReduce() throws Exception {
    Configurator.getDefaultConfigurator().configure();
    
    BasicDBObject query = new BasicDBObject("test1", "fao");
    query.put("metadata.mimetype.value", "application/pdf");
    
    HistogramJob job = new HistogramJob("test1", "format", query);
    MapReduceOutput output = job.execute();
    
    System.out.println(output);
    Assert.assertNotNull(output);
  }
  
  @Test
  public void myTestHistogramMapReduce() throws Exception {
	    Configurator.getDefaultConfigurator().configure();
	    
	    HistogramJob job = new HistogramJob("test1", "format");
	    MapReduceOutput output = job.execute();
	    
//	    BasicDBObject q = new BasicDBObject();
//	    PersistenceLayer pl = Configurator.getDefaultConfigurator().getPersistence();
//	    DBCollection col = pl.getDB().getCollection(Constants.TBL_ELEMENTS);
//	    DBCursor cur = col.find();
//	    while (cur.hasNext()) {
//	    	log.debug(cur.next().toString());	    	
//	    }
	  
	    System.out.println(output);
	    Assert.assertNotNull(output);
  }
  
  
}
