package controllers;

import helpers.BaseGraph;
import helpers.BubbleGraph;
import helpers.Graph;
import helpers.GraphData;
import helpers.Statistics;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.node.ObjectNode;

import net.sf.ehcache.search.Results;

import play.Logger;
import play.data.DynamicForm;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.overview;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.petpet.c3po.analysis.mapreduce.BubbleChartJob;
import com.petpet.c3po.analysis.mapreduce.JobResult;
import com.petpet.c3po.common.Constants;
import com.petpet.c3po.datamodel.Filter;
import com.petpet.c3po.utils.Configurator;
import com.petpet.c3po.utils.DataHelper;
import common.WebAppConstants;

public class Overview extends Controller {

  public static Result index() {
    final List<String> names = Application.getCollectionNames();
    Filter filter = Application.getFilterFromSession();

    Statistics stats = null;
    GraphData data = null;
    if (filter != null) {
      BasicDBObject ref = new BasicDBObject("descriminator", filter.getDescriminator());
      ref.put("collection", session(WebAppConstants.CURRENT_COLLECTION_SESSION));
      DBCursor cursor = Configurator.getDefaultConfigurator().getPersistence().find(Constants.TBL_FILTERS, ref);

      Logger.info("filter is not null");
      if (cursor.count() == 1) { // only root filter
        Logger.info("filter has no parent, using cached statistics");
        // used cached results
        stats = FilterController.getCollectionStatistics(filter.getCollection());
        data = Overview.getDefaultGraphs(filter, true);
      } else {
        // calculate new results
        stats = FilterController.getCollectionStatistics(filter);

        List<String> properties = new ArrayList<String>();
        while (cursor.hasNext()) {
          Filter tmp = DataHelper.parseFilter(cursor.next());
          if (tmp.getType() == null || tmp.getType().equals("null")) {
        	  if (tmp.getProperty() != null) {
                  properties.add(tmp.getProperty());
                }
          }
          if (tmp.getType().equals("bubblegraph")) {
        	  String p1 = tmp.getBubbleProperty(0);
        	  String p2 = tmp.getBubbleProperty(1);
        	  if (p1 != null) {
                  properties.add(p1);
              }
        	  if (p2 != null) {
        		  properties.add(p2);
        	  }
          }
          
        }
        data = Overview.getAllGraphs(filter, properties);
      }

    }
    return ok(overview.render(names, data, stats));
  }

  public static Result getGraph(String property) {

    // if it is one of the default properties, do not draw..
    for (String p : Application.PROPS) {
      if (p.equals(property)) {
        return ok();
      }
    }

    Graph g = FilterController.getGraph(property);

    return ok(play.libs.Json.toJson(g));
  }
  
  public static Result getBubbleGraph(String property1, String property2) {
	DynamicForm form = form().bindFromRequest();
	String alg1 = form.get("alg1");
	String width1 = form.get("width1");
	String alg2 = form.get("alg2");
	String width2 = form.get("width2");
	  
	BaseGraph g = FilterController.getBubbleGraph(property1, property2,
			alg1, width1, alg2, width2);
    response().setContentType("application/json");
    
    if (g == null)
    	return noContent();
    
    ObjectNode result = Json.newObject();
    result.put("type", g.getType());
    result.put("title", g.getTitle());
    // for some reason this parser doesn't like ', so convert it to "
    
    result.put("graphData", Json.parse(g.getGraphData()
    		.replace("\\", "\\\\")
    		.replace("'", "\"")));
    result.put("graphOptions", Json.parse(g.getGraphOptions()
    		.replace("\\", "\\\\")
    		.replace("'", "\"")));
    result.put("options", play.libs.Json.toJson(g.getOptions()));
    
	return ok(result);
  }

  private static GraphData getDefaultGraphs(Filter f, boolean root) {
    List<BaseGraph> graphs = new ArrayList<BaseGraph>();
    for (String prop : Application.PROPS) {
      Graph graph;
      if (root) {
        graph = FilterController.getGraph(f.getCollection(), prop);
      } else {
        graph = FilterController.getGraph(f, prop);
      }

      graphs.add(graph);

      // only for testing...
      //graphs.add(FilterController.getBubbleGraph(f, "format", "created"));
      
      // TODO decide when to cut long tail...
    }

    return new GraphData(graphs);
  }

  private static GraphData getAllGraphs(Filter f, List<String> props) {
    GraphData graphs = getDefaultGraphs(f, false);

    for (String prop : props) {
      boolean found = false;
      for (String def : Application.PROPS) {
        if (prop.equals(def)) {
          found = true;
          break;
        }
      }

      if (!found) {
        Graph graph = FilterController.getGraph(f, prop);
        graphs.getGraphs().add(graph);
      }
    }

    return graphs;
  }

}
