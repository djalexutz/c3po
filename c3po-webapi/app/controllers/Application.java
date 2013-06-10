package controllers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.index;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCursor;
import com.mongodb.MapReduceOutput;
import com.petpet.c3po.analysis.mapreduce.CollectionPropertiesJob;
import com.petpet.c3po.analysis.mapreduce.JobResult;
import com.petpet.c3po.analysis.mapreduce.MapReduceJob;
import com.petpet.c3po.api.dao.PersistenceLayer;
import com.petpet.c3po.common.Constants;
import com.petpet.c3po.datamodel.Filter;
import com.petpet.c3po.datamodel.Property;
import com.petpet.c3po.utils.Configurator;
import com.petpet.c3po.utils.DataHelper;
import common.WebAppConstants;

public class Application extends Controller {

  public static final String[] PROPS = { "mimetype", "format", "format_version", "valid", "wellformed",
      "creating_application_name", "created" };

  public static Result index() {

    buildSession();

    return ok(index.render("c3po", getCollectionNames()));
  }

  public static Result setCollection(String c) {
    Logger.debug("Received collection setup change for " + c);
    final PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    final List<String> names = getCollectionNames();

    if (c == null || c.equals("") || !names.contains(c)) {
      return notFound("No collection '" + c + "' was found");
    }

    Filter f = new Filter(c, null, null);

    buildSession();
    String session = session(WebAppConstants.SESSION_ID);
    System.out.println("session: " + session);
    f.setDescriminator(session);

    BasicDBObject query = new BasicDBObject("collection", c);
    query.put("descriminator", session);
    System.out.println("Query: " + query.toString());

    DBCursor cursor = p.find(Constants.TBL_FILTERS, query);
    if (cursor.count() == 0) {
      p.insert(Constants.TBL_FILTERS, f.getDocument());
    } else {
      f = DataHelper.parseFilter(cursor.next());
    }

    session().put(WebAppConstants.CURRENT_COLLECTION_SESSION, c);
    session().put(WebAppConstants.CURRENT_FILTER_SESSION, f.getDescriminator());
    return ok("The collection was changed successfully\n");
  }

  public static Result setSetting() {

    DynamicForm form = form().bindFromRequest();
    String setting = form.get("setting");
    String value = form.get("value");

    session().put(setting, value);
    Logger.debug("changed setting '" + setting + "' to value: '" + value + "'");
    return ok();
  }

  public static Result getSetting(String key) {
    String value = session(key);
    return ok(play.libs.Json.toJson(value));
  }

  public static Result getCollections() {
    Logger.debug("Received a get collections call");
    final String accept = request().getHeader("Accept");

    if (accept.contains("*/*") || accept.contains("application/xml")) {
      return collectionsAsXml();
    } else if (accept.contains("application/json")) {
      return collectionsAsJson();
    }

    return badRequest("The accept header is not supported");
  }

  public static Result getProperties() {
    Logger.debug("Received a get properties call");
    final String accept = request().getHeader("Accept");

    if (accept.contains("*/*") || accept.contains("application/xml")) {
      return TODO;
    } else if (accept.contains("application/json")) {
      return propertiesAsJson();
    }

    return badRequest("The accept header is not supported");
  }

  public static Result getProperty(String name) {
    Logger.debug("Received a get property call");
    final String accept = request().getHeader("Accept");
    if (accept.contains("*/*") || accept.contains("application/json")) {
      return propertyAsJson(name);
    } else {
      return TODO;
    }
  }

  private static Result propertyAsJson(String name) {
    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    Property property = p.getCache().getProperty(name);
    return ok(play.libs.Json.toJson(property));
  }

  private static Result propertiesAsJson() {
    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    List<String> properties = new ArrayList<String>();

    String collection = session(WebAppConstants.CURRENT_COLLECTION_SESSION);
    MapReduceJob job = new CollectionPropertiesJob(collection);
    JobResult output = job.run();

    List<BasicDBObject> results = output.getResults();
    if (results != null && results.size() > 0) {
      for (BasicDBObject dbo : results) {
        properties.add((String) dbo.get("_id"));
      }
    } else {
      properties.addAll(p.distinct(Constants.TBL_PROEPRTIES, "key"));
    }

    return ok(play.libs.Json.toJson(properties));
  }

  public static Result collectionsAsXml() {
    final List<String> names = Application.getCollectionNames();
    names.remove(0);
    final StringBuffer resp = new StringBuffer("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");

    resp.append("<collections>\n");

    for (String s : names) {
      resp.append("<collection name=\"" + s + "\" />\n");
    }

    resp.append("</collections>\n");

    response().setContentType("text/xml");

    return ok(resp.toString());
  }

  public static Result collectionsAsJson() {
    final List<String> names = Application.getCollectionNames();
    names.remove(0);
    response().setContentType("application/json");
    return ok(play.libs.Json.toJson(names));
  }

  public static List<String> getCollectionNames() {
    PersistenceLayer persistence = Configurator.getDefaultConfigurator().getPersistence();
    List<String> collections = (List<String>) persistence.distinct(Constants.TBL_ELEMENTS, "collection");
    Collections.sort(collections);
    collections.add(0, "");
    return collections;

  }

  public static Filter getFilterFromSession() {
    PersistenceLayer pl = Configurator.getDefaultConfigurator().getPersistence();
    String f = session(WebAppConstants.CURRENT_FILTER_SESSION);
    String c = session(WebAppConstants.CURRENT_COLLECTION_SESSION);

    if (f != null) {
      BasicDBObject fQuery = new BasicDBObject("descriminator", f);
      fQuery.put("collection", c);
	      fQuery.put("property0", null);
	      fQuery.put("value0", null);
	      fQuery.put("property1", null);
	      fQuery.put("value1", null);
    	  fQuery.put("property", null);
    	  fQuery.put("value", null);
    	  fQuery.put("filterID", null);
      
      DBCursor cursor = pl.find(Constants.TBL_FILTERS, fQuery);
      if (cursor.count() == 1) {
        Filter filter = DataHelper.parseFilter(cursor.next());
        return filter;
      } else if (cursor.count() > 1) {
        Logger.error("More than one filter found");
        throw new RuntimeException("Found more than one filters with the same id");
      }
    }
    return null;
  }


  public static BasicDBObject getFilterQuery(Filter filter) {
    return DataHelper.getFilterQuery(filter);
  }

  public static Result clear() {
    session().clear();

    final PersistenceLayer pl = Configurator.getDefaultConfigurator().getPersistence();
    pl.getDB().getCollection(Constants.TBL_FILTERS).drop();

    return redirect("/c3po");
  }

  private static Object inferValue(String value) {
    Object result = value;
    if (value.equalsIgnoreCase("true")) {
      result = new Boolean(true);
    }

    if (value.equalsIgnoreCase("false")) {
      result = new Boolean(false);
    }

    return result;
  }

  private static void buildSession() {
    String session = session(WebAppConstants.SESSION_ID);
    if (session == null) {
      session(WebAppConstants.SESSION_ID, UUID.randomUUID().toString());
    }
  }
}