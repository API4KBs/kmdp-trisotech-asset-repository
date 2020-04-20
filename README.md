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

To compile locally requires using the token. From the commandline, this is easiest to do with a local properties file that just includes the token:

`mvn -U clean install -Dmaven.javadoc.skip=true -Dspring.profiles.active=dev -Dspring.config.additional-location="file:<localpath>\tt.properties"`

The file can be named anything as long as it ends in .properties.

From within IntelliJ, the appropriate properties files need to be modified to include the token.
This is needed to be able to debug within IntelliJ.
 
** WARNING **
DO NOT CHECK-IN THESE FILES WITH THE TOKEN!!!!
