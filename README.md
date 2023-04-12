# API4KP - Trisotech Digital Enterprise Suite Facade

Note: this software is provided as-is by Mayo Clinic. Trisotech Inc. is aware of
this project at the time this is published, but has no other relationship with this codebase.

The Trisotech Knowledge Platform Wrapper (TTW) is a partial implementation of the API4KP
specification, backed by an adapter built on the Trisotech Digital Enterprise Suite (TT DES) public
API and Graph (SPARQL) API.
This implementation is primarily focused on discovering and retrieving Knowledge Assets, expressed
as BPM+ models. The server constructs API4KP conformant metadata, and redacts some of the
dependencies on the DES proprietary extensions to the standards.

## News and Noteworthy

- Implementation of the API4KP Asset Repository API, focused on access/read operations
    - enable discovery of Models via computable metadata, linked-data style
    - basic auto-generated UI with link navigation
- (NEW 6.0!) Implementation of the API4KP Artifact Repository, focused on access/read operations
    - enable direct retrieval of Models, and/or specific versions thereof
- (NEW 6.0!) Support for multiple Places
    - enables management across multiple teams and products
- (NEW 6.0!)Improved caching mechanism
    - it's faster!
- (NEW 6.0!) KEM models as Knowledge Assets
    - exported as OMG's Meta Vocabulary Facility (MVF) models
- (NEW 6.0!) Support for Decision/Process services as Knowledge Assets
    - Knowledge 'at rest' + Knowledge 'in motion'
- (NEW 6.0!) KEM-annotated decision models
  - Build CSV Concepts directly in KEM
- (NEW 6.0!) Cloud Event support for DES webhooks
  - Sync TTW and DES

### Upcoming

- Content Negotiation: support for DMN 1.3 and later versions
- Configurability of Service Library runtimes

### Experimental

- KEM to SCG to OWL integration
  - Build CSO definitions, build in the OWL to SKOS to FHIR pipeline, to integrate with SPE
- cCPM PlanDefinition preview endpoint (/ccpms)
  - Relies on CMMN case models with asset type "Clinical Case Management Model"
  - SME to Screen is back !
- cCPM Validation rules (/validate/ccpms)
  - CKE "method & style" for cCPMs

## User Instructions

### Prerequisites

* The TTW application requires a licensed instance of the Trisotech DES suite. The DES instance must
  support the Trisotech Knowledge Graph SPARQL endpoint.

### Build

The server is a Spring Boot application compatible with Java 11 and Tomcat 9.x.

To build, use Maven with a repository that contains the API4KP KMDP implementation jars
`mvn clean install`

### Configuration

The following properties need to be configured as (Spring) application properties:

* `edu.mayo.kmdp.trisotechwrapper.baseUrl` - the base URL of the server instance,
  e.g. https://bpm-health.trisotech.com/
* `edu.mayo.kmdp.trisotechwrapper.trisotechToken` - the API bearer token used to connect to the DES
  instance, obtainable from the DES instance itself.
* `edu.mayo.kmdp.trisotechwrapper.place.paths` - a comma-separated list of {placeId}/{placePath}
  pointing to the Places/Folders that will be exposed through the TTW API
* `edu.mayo.kmdp.trisotechwrapper.executionEnv` - a comma-separated list of (names of) execution
  environments, in the ServiceLibrary associated to the DES base URL (TODO!), used to look up
  deployed services

* DEPRECATED `edu.mayo.kmdp.trisotechwrapper.repositoryId` - the UUID of the target Place to pull
  Assets from
* DEPRECATED `edu.mayo.kmdp.trisotechwrapper.repositoryName` - the name of the target Place to pull
  Assets from
* DEPRECATED `edu.mayo.kmdp.trisotechwrapper.repositoryPath` - the path of the folder within the
  Place to pull Assets from

### Assets and Service Assets

- Each model (version) will be available as a Knowledge Artifact through the Artifact APIs
- Each model (version) will be considered the carrier of an Anonymous Knowledge Asset, and become
  available through the Asset APIs, if the feature
  flag `edu.mayo.kmdp.application.flag.allowAnonymous` is set to TRUE
    - Models can be associated to _Named_ assets, asserting a Custom Attribute "knowledgeAssetId" at
      the Model level
        - Named Asset IDs must be URIs, which follow the patterns:
            - urn:uuid:{uuid}:{versionTag}
            - {base}/assets/{uuid}/versions/{versionTag}
                - {base} defaults to "http://clinicalknowledgemanagement.mayo.edu/assets" and can be
                  overwritten setting the environment
                  variable `edu.mayo.kmdp.trisotechwrapper.assetNamespace`
- Decision Services in DMN Decision Models will be exposed as Service Assets (0..N per Model)
    - Auto-generated Decision Services for whole models and diagrams are excluded
    - _Named_ Service Assets are obtained adding the Custom Attribute "serviceAssetId" at the level
      of the Decision Service
      - Same rules for Named Asset IDs apply
- BPMN Process can only be exposed as Named Service Assets (0..1 per Model)
    - _Named_ Service Assets are obtained adding the Custom Attribute "serviceAssetId" at the level
      of the Process (Page) that contains the root Process to be executed
      - Same rules for Named Asset IDs apply

### KEM as an Ontology Tool

- Create a KEM model
  - (optional) assign a knowledgeAssetId
  - Define ontology prefixes as customAttribute
    - name : "prefix"
    - value : "{prefix}: <{namespace}>" 
      - Example value "CSV: <https://ontology.mayo.edu/taxonomies/clinicalsituations#>"
  - Create a KEM Term
    - Add a tag that matches the ontology prefix, e.g. "CSV"

