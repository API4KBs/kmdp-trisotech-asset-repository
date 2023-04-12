package edu.mayo.kmdp.trisotechwrapper.components.operators;

import edu.mayo.kmdp.trisotechwrapper.models.kem.v5.KemConcept;
import java.util.Optional;
import javax.annotation.Nonnull;


/**
 * Helper class used to handle data shapes and data types
 * <p>
 * Placeholder, waiting for SDMN support
 */
public final class DataShapeHelper {

  /**
   * Thhe URL Pattern for FHIR R4 profiles associated to base resource types
   */
  private static final String FHIR_BASE_PROFILES =
      "https://www.hl7.org/fhir/R4/%s.profile.json";

  /**
   * Private Constructor
   */
  private DataShapeHelper() {
    // functions only
  }

  /**
   * Mpas an internal itemDefinition name to a datatype URI, if possible
   *
   * @param kc the KEM Concept
   * @return the datatype fof the URI
   */
  @Nonnull
  public static String getDataDefinition(
      @Nonnull final KemConcept kc) {
    var ref = kc.getProperties().getTypeRef();

    if (hasFHIRDatatype(kc)) {
      return getFHIRProfile(kc);
    }

    return ref;
  }

  /**
   * Constructs the URL for the StructureDefinition of the base Resource Type associated to a KEM
   * Concept
   *
   * @param kc the KEM Concept
   * @return the URL of the FHIR R4 profile
   */
  private static String getFHIRProfile(KemConcept kc) {
    var fhirType = Optional.ofNullable(kc.getProperties().getTypeRef())
        .map(ref -> ref.substring(4))
        .orElseGet(() -> kc.getProperties().getResource())
        .toLowerCase();
    return String.format(FHIR_BASE_PROFILES, fhirType);
  }

  /**
   * Detects whether a KEM Concept is associated to a FHIR datatype
   * <p>
   * Checks for itemDefinitions in the FHIR accelerator, and/or the presence of a Resource type
   *
   * @param kc the KEM Concept
   * @return true if the Concepct is associated to a FHIR datatype or, in absence of a datatype, the
   * association to a FHIR resource
   */
  private static boolean hasFHIRDatatype(KemConcept kc) {
    return Optional.ofNullable(kc.getProperties().getTypeRef())
        .map(ref -> ref.startsWith("fhir"))
        .orElseGet(() -> kc.getProperties().getResource() != null);
  }


}
