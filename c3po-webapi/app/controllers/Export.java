package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import play.Logger;
import play.data.DynamicForm;
import play.mvc.Controller;
import play.mvc.Result;
import views.html.export;

import com.mongodb.BasicDBObject;
import com.petpet.c3po.analysis.CSVGenerator;
import com.petpet.c3po.analysis.ProfileGenerator;
import com.petpet.c3po.analysis.RepresentativeAlgorithmFactory;
import com.petpet.c3po.analysis.RepresentativeGenerator;
import com.petpet.c3po.api.dao.PersistenceLayer;
import com.petpet.c3po.common.Constants;
import com.petpet.c3po.datamodel.ActionLog;
import com.petpet.c3po.datamodel.Filter;
import com.petpet.c3po.utils.ActionLogHelper;
import com.petpet.c3po.utils.Configurator;

public class Export extends Controller {

  public static Result index() {
    return ok(export.render("c3po - Export Data", Application.getCollectionNames()));
  }

  public static Result profile() {
    Logger.debug("Received a profile generation call");
    final String accept = request().getHeader("Accept");

    final DynamicForm form = form().bindFromRequest();
    final String c = form.get("collection");
    final String e = form.get("includeelements");

    Filter filter = Application.getFilterFromSession();
    boolean include = false;

    if (filter == null) {
      if (c == null) {
        return badRequest("No collection parameter provided\n");
      } else if (!Application.getCollectionNames().contains(c)) {
        return notFound("No collection with name " + c + " was found\n");
      }

      filter = new Filter(c, null, null);
    }

    if (e != null) {
      include = Boolean.valueOf(e);
    }

    if (accept.contains("*/*") || accept.contains("application/xml")) {
      return profileAsXml(filter, include);
    }

    Logger.debug("The accept header is not supported: " + accept);
    return badRequest("The provided accept header '" + accept + "' is not supported");
  }

  public static Result exportAllToCSV() {
    CSVGenerator generator = getGenerator();

    Filter filter = Application.getFilterFromSession();

    String collection = filter.getCollection();
    String path = "exports/" + collection + "_" + filter.getDescriminator() + "_matrix.csv";
    generator.exportAll(collection, path);

    File file = new File(path);

    try {
      return ok(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      return internalServerError(e.getMessage());
    }
  }

  public static Result exportFilterToCSV() {
    CSVGenerator generator = getGenerator();

    Filter filter = Application.getFilterFromSession();

    String collection = filter.getCollection();
    String path = "exports/" + collection + "_" + filter.getDescriminator() + "_matrix.csv";
    generator.export(filter, path);

    File file = new File(path);

    try {
      return ok(new FileInputStream(file));
    } catch (FileNotFoundException e) {
      return internalServerError(e.getMessage());
    }
  }

  private static CSVGenerator getGenerator() {
    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    CSVGenerator generator = new CSVGenerator(p);

    return generator;

  }

  private static Result profileAsXml(Filter filter, boolean includeelements) {
    File result = generateProfile(filter, includeelements);

    return ok(result);
  }

  private static File generateProfile(Filter filter, boolean includeelements) {
    StringBuilder pathBuilder = new StringBuilder();
    pathBuilder.append("profiles/").append(filter.getCollection()).append("_").append(filter.getDescriminator());
    if (includeelements) {
      pathBuilder.append("_").append("elements");
    }

    pathBuilder.append(".xml");

    String path = pathBuilder.toString();

    Logger.debug("Looking for collection profile " + path);

    File file = new File(path);

    if (!file.exists() || isCollectionUpdated(filter.getCollection()) || isNewFilter(file, filter)) {
      Logger.debug("File does not exist. Generating profile for filter " + filter.getDocument());
      Configurator configurator = Configurator.getDefaultConfigurator();
      PersistenceLayer p = configurator.getPersistence();
      String alg = configurator.getStringProperty("c3po.samples.algorithm");
      RepresentativeGenerator samplesGen = new RepresentativeAlgorithmFactory().getAlgorithm(alg);
      ProfileGenerator generator = new ProfileGenerator(p, samplesGen);
      Document profile = generator.generateProfile(filter, includeelements);

      generator.write(profile, path);
      file = new File(path);

      ActionLogHelper alHelper = new ActionLogHelper(p);
      alHelper.recordAction(new ActionLog(filter.getCollection(), ActionLog.PROFILE_ACTION));
    }

    return file;
  }

  private static boolean isCollectionUpdated(String collection) {
    PersistenceLayer p = Configurator.getDefaultConfigurator().getPersistence();
    ActionLogHelper alHelper = new ActionLogHelper(p);
    ActionLog lastAction = alHelper.getLastAction(collection);

    boolean isUpdated = true;

    if (lastAction != null) {
      if (lastAction.getAction().equals(ActionLog.PROFILE_ACTION)) {
        isUpdated = false;
      }
    }

    return isUpdated;
  }

  private static boolean isNewFilter(File file, Filter filter) {
    long profileFiltersCount = -1;
    long profileObjectsCount = -1;
    
    boolean isNew = true;
    try {
      final SAXReader reader = new SAXReader();
      Document doc = reader.read(file);
      Element partition = doc.getRootElement().element("partition");
      profileFiltersCount = partition.element("filter").element("parameters").elements().size();
      profileObjectsCount = Long.parseLong(partition.attributeValue("count"));

    } catch (final DocumentException e) {
      //do nothing...
      //just regenerate the profile...
    }

    PersistenceLayer persistence = Configurator.getDefaultConfigurator().getPersistence();
    long filtersCount = persistence.count(Constants.TBL_FILTERS,
        new BasicDBObject("descriminator", filter.getDescriminator()));
    long filterObjectsCount = persistence.count(Constants.TBL_ELEMENTS, Application.getFilterQuery(filter));

    if (filtersCount == profileFiltersCount && profileObjectsCount == filterObjectsCount) {
      isNew = false;
    }
    
    return isNew; 

  }

}
