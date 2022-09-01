# Knowledge Asset Repository - Trisotech Digital Enterprise Suite Facade

Note: this software is provided as-is by Mayo Clinic. Trisotech Inc. is aware of
this project at the time this is published, but has no other relationship with this codebase.

The Trisotech Knowledge Asset Repository is a partial implementation of the API4KP Knowledge Asset
Repository API, backed by an adapter built on the Trisotech Digital Enterprise Suite (TT DES) public
API.
This implementation is primarily focused on discovering and retrieving Knowledge Assets, expressed
as BPM+ models. The server constructs API4KP conformant metadata, and redacts some of the
dependencies
on the DES proprietary extensions to the standards.

The main components of this implementation are:

1. The wrapper
2. The weaver
3. The metadata extractor

### The wrapper

The TrisotechWrapper 'wraps' the native Trisotech API, handling authentication/authorization,
constructing the HTTP ReST calls, and processing the responses.

### The weaver (+ redactor)

The weaver rewrites references to external entities (e.g. ontology concepts) and/or non-standard
extensions with localized, standardized values.

### The metadata extractor (introspector)

The extractor uses the information contained in a BPM+ Knowledge Artifact ("model"), as well
as some of the DES internal metadata, to create KnowledgeAsset surrogates

## User Instructions

### Prerequisites

* The Wrapper requires a licensed instance of the Trisotech DES suite. The DES instance must support
  the Trisotech Knowledge Graph SPARQL endpoint.
* The current implementation can only access one Place at a time, and can be configured to further
  target a specific folder within that Place.
* Only CMMN and DMN models are supported at this point - BPMN coming soon !

IMPORTANT: To qualify as an API4KP Knowledge Assets, models must have exactly one model element
annotated with a Custom attribute.
The attribute name must be `knowledgeAssetId` and the value must be a URI that follows the pattern
`{Base URL}/assets/{UUID}/versions/{SemVer Tag}`. Each model can not have more than one asset ID,
and no two models in the same place can share the same asset ID.

These constraints are considered subject to change

### Build

The server is a Spring Boot application compatible with Java 11 and Tomcat 9.x.

To build, use Maven with a repository that contains the API4KP KMDP implementation jars
`mvn clean install`

### Configuration

The following properties need to be configured as (Spring) application properties:

* `edu.mayo.kmdp.trisotechwrapper.baseUrl` - the base URL of the server instance,
  e.g. https://bpm-health.trisotech.com/
* `edu.mayo.kmdp.trisotechwrapper.repositoryId` - the UUID of the target Place to pull Assets from
* `edu.mayo.kmdp.trisotechwrapper.repositoryName` - the name of the target Place to pull Assets from
* `edu.mayo.kmdp.trisotechwrapper.repositoryName` - the path of the folder within the Place to pull
  Assets from
* `edu.mayo.kmdp.trisotechwrapper.trisotechToken` - the API bearer token used to connect to the DES
  instance, obtainable from the DES instance itself. 



