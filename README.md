# raven-repository
Raven R package snapshot repository

======

Google app engine snapshot repository, use `/registry/cran`, `/registry/bioc-devel`,
and `/registry/bioc-release` URIs to initialize with cran and bioc packages. Will
accept github push webhooks at `/registry/github`. After a package snapshot is
taken you can see available versions at /repository/:package which comes back as a
json list of version names. Version details can be obtained at
`/repository/:package/:version`. The json will contain satisfactory dependency
versions (if there are any) available via this repository. The snapshot contains
enough information to retrieve the package version source.

To register a package with the repository the source needs to be hosted on github, go
to the repository page then choose `Settings > Webhooks & Services > Add Webhook`, put
`http://raven-repository.appspot.com/registry/github` for the payload URL and
`application/json` for content type, leave the secret field blank. Your package will
be registered with the repository the next time you make a commit. Once a snapshot is
taken it is immutable, no updates are allowed. You need to bump the version in the
`DESCRIPTION` file for another snapshot to take place.
