package com.petpet.c3po.analysis.mapreduce;

import static org.junit.Assert.*;

import java.util.Iterator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DBObject;
import com.mongodb.MapReduceOutput;
import com.petpet.c3po.utils.Configurator;

public class BubbleChartJobTest {
	private static final Logger log = LoggerFactory.getLogger(BubbleChartJobTest.class);
	
	@Before
	public void setup() {
		Configurator.getDefaultConfigurator().configure();
	}
	
	@After
	public void tearDown() {
		
	}
	
	
	@Test
	public void BubbleChartTwoStringPropertiesTest() {
		BubbleChartJob job = new BubbleChartJob("test", "format", "mimetype");
		MapReduceOutput output = job.execute();
		
		log.debug(output.toString());
		
		for (Iterator<DBObject> it = output.results().iterator(); it.hasNext(); ) {
			log.debug("object: {}", it.next().toString());
		}
		assertNotNull(output);
	}

	@Test
	public void BubbleChartPropertiesStringDateTest() {
		BubbleChartJob job = new BubbleChartJob("test", "format", "created");
		MapReduceOutput output = job.execute();
		
		log.debug(output.toString());
		
		for (Iterator<DBObject> it = output.results().iterator(); it.hasNext(); ) {
			log.debug("object: {}", it.next().toString());
		}
		assertNotNull(output);
	}

	@Test
	public void BubbleChartPropertiesStringNumericTest() {
		BubbleChartJob job = new BubbleChartJob("test", "fnumber", "pagecount");
		job.getConfig().put("bin_width", "500000");
		MapReduceOutput output = job.execute();
		
		log.debug(output.toString());
		
		for (Iterator<DBObject> it = output.results().iterator(); it.hasNext(); ) {
			log.debug("object: {}", it.next().toString());
		}
		assertNotNull(output);
	}

}
