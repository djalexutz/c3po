package com.petpet.c3po.adaptor.tika;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class TIKAHelper {

  private static Properties TIKA_PROPS;

  public static void init() {
    try {
      InputStream in = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("tika_property_mapping.properties");
      TIKA_PROPS = new Properties();
      TIKA_PROPS.load(in);
      in.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  
  /**
   * For now we support only the specified properties within the file
   * as the TIKA adaptor is still experimental
   * @param name the name of the property
   * @return the normalised property key corresponding to TIKAs property name
   */
  public static String getPropertyKeyByTikaName(String name) {
    final String prop = (String) TIKA_PROPS.get(name);
    return prop;
  }
  
 
}
