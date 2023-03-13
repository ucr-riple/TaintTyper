package edu.ucr.cs.riple.taint.ucrtainting.serialization;

import org.json.JSONObject;

/** Interface for classes that can be serialized to JSON. */
public interface JSONSerializable {

  /**
   * Returns the object representation in json format.
   *
   * @return Object as json.
   */
  JSONObject toJSON();
}
