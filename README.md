# raven-repository
Google app engine based snapshot repository for R package metadata. Instance available
at `http://raven-repository.appspot.com/`

======

The repository takes its own metadata snapshots of cran and bioconductor software
packages (not to be confused with bioconductor data or annotation packages) and can
also accept packages from github

To register a package with the repository the source needs to be hosted on github, go
to the repository page then choose `Settings > Webhooks & Services > Add Webhook`, put
`http://raven-repository.appspot.com/registry/github` for the payload URL and
`application/json` for content type, leave the secret field blank. Your package will
be registered with the repository the next time you make a commit. Once a snapshot is
taken it is immutable, no updates are allowed. You need to bump the version in the
`DESCRIPTION` file for another snapshot to take place. List of available versions for
a given package is available at `/repository/:package`. Version details about a snapshot
can be obtained at `/repository/:package/:version`. The json will contain satisfactory
dependency versions (if there are any) available via this repository. The snapshot
contains enough information to retrieve the package version source using functionality
provided by `devtools` and versions of all dependencies at the time of the snapshot,
for example:

```
{
  "@c":".Svn",
  "timestamp":1426746005301,
  "url":"https://hedgehog.fhcrc.org/bioconductor",
  "dir":"madman/Rpacks/topGO",
  "user":"readonly",
  "pw":"readonly",
  "revision":"99986",
  "depends":{
    "graph":"1.45.2",
    "BiocGenerics":"0.13.7",
    "GO.db":"3.0.0",
    "SparseM":"1.6",
    "AnnotationDbi":"1.29.17",
    "Biobase":"2.27.2"
  },
  "imports":{
    "graph":"1.45.2",
    "lattice":"0.20-30",
    "SparseM":"1.6",
    "AnnotationDbi":"1.29.17",
    "Biobase":"2.27.2"
  }
}
```

Is the output for `http://raven-repository.appspot.com/repository/topGO/2.19.1` which
notes the package depends and imports a series of other packages with their versions
(whose meta data is available at the appropriate URIs), the package itself is available
from an svn server at the listed root URL, in the appropriate subfolder, with specified
credentials at the given revision. Notice the absence of branch field denoting the
package is in trunk. Empty fields are not transmitted, the minimum information for an
svn hosted package is URL and revision. Packages may also come from cran - in which case
the package name and version are echoed back, or github - in which case the json will
contain the github repo and the sha of the commit. Timestamp is amount of milliseconds
since beginning of time at the moment of snapshot resolution

======

To launch your own repository you need Java 7 and maven 3.1, clone the source and
launch a local server with `mvn appengine:devserver`. To initialize cran and
bioconductor package metadata hit `/registry/cran`, `/registry/bioc-devel`,
and `/registry/bioc-release` URIs (you'll need to remove security constraints found
in src/main/webapp/WEB-INF/web.xml prior to launching the server). All of those should
take about 30 seconds together, dependent on how tired the hamsters at cran and bioc
are at the time of the snapshot. After a package snapshot is taken you can see
available versions at /repository/:package which comes back as a json list of version
names. Datastore should be located at `WEB-INF/local_db.bin`, the metadata for a
snapshot of all cran and all bioc with both devel and release branches weighs in at
about 5MB (that's an M)