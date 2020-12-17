# Trisotech Asset Repository

The Trisotech Asset Repository is a wrapper around the Trisotech modeling web application.
The code is meant to retrieve models from Trisotech and wrap them with the appropriate annotations to allow them to be placed in the MEA Asset Repository.
The main components of the code are:
1. The wrapper
2. The weaver
3. The extractor

### The wrapper

The TrisotechWrapper provides methods to access the data from Trisotech. It 'wraps' the Trisotech API.
It is used primarily by the TrisotechAssetRepository which implements the Asset Repository interfaces.
Much of the data is handled as a TrisotechFileInfo object. This object maps to the data returned from the Trisotech APIs.
This is the same data returned to Postman.

### The weaver

The weaver takes the model file from Trisotech and parses through it to modify or 'weave' the data.
For example, it will remove all the 'trisotech' tags from the file.
Some will be replaced with KMDP-specific tags and information.
Once this is all done, this woven file is the document fed to the extractor and returned in the surrogate.

### The extractor

The extractor takes the output of the weaver plus the metadata (the TrisotechFileInfo) of the model and creates a surrogate.
There are multiple pieces to the extractor:
1. IdentityMapper
    * IdentityMapper has information from Trisotech that was gathered from the SPARQL API provided by Trisotech. There are 3 different queries of data and they are all performed at service start up.
2. MetadataExtractor
    * Ties the mapper and the TrisotechExtractionStrategy together. Much of it is a wrapper for the strategy.
3. TrisotechExtractionStrategy
    * Handles the creation of the surrogate.
    * Makes use of the wrapper and the mapper.
    * Gets the output of the weaver.
    

### Compiling

To compile locally requires using the token. 

Create a user environment variable with the name:
`edu.mayo.kmdp.trisotechwrapper.trisotechToken`

Give it the token value for its value.
Then use the following to compile on the command line:
`mvn clean install`

ALTERNATIVELY: From the commandline, this can be done with a local properties file that just includes the token:
`mvn -U clean install -Dmaven.javadoc.skip=true -Dspring.profiles.active=dev -Dspring.config.additional-location="file:<localpath>\tt.properties"`

The file can be named anything as long as it ends in .properties.

There is no longer a need to modify the project properties files. Instead use the environment variable or a .spring-tools.devtools.properties file.
IF one of the properties file is modified to include the token, heed the warning:

** WARNING **
DO NOT CHECK-IN THESE FILES WITH THE TOKEN!!!!
