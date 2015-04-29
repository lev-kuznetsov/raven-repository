# raven-repository

Meta repository for versioned R packages

===

You can deploy your own snapshot repository on openshift; sign up for openshift
and download the `rhc` tool, then (here I named the application `raven`):

```
rhc app create raven diy                        # Create application
rhc cartridge add postgresql-9.2 -a raven       # Add the persistence cartridge
rhc app stop -a raven                           # Stop it
cd raven
git rm -r .openshift README.md diy misc         # Remove templates
git commit -am "removed templates"
git remote add upstream https://github.com/dfci-cccb/raven-repository
git pull -s recursive -X theirs upstream master # Clone this repo
git push                                        # Deploy
```

After that you can point at `http://raven-<domain>.rhcloud.com/repository` and
greet an empty `[]`. Snapshots are taken every morning just after 2AM, wait for
the next day for packages to show up. Once snapshots take place you can query
for available packages at `http://raven-<domain>.rhcloud.com/repository`, query
for available snapshots at 
`http://raven-<domain>.rhcloud.com/repository/<package>`, and see the meta
information for obtaining the package at
`http://raven-<domain>.rhcloud.com/repository/<package>/<version>`. You can
also query by ISO date instead of the version, for example:
`http://raven-<domain>.rhcloud.com/repository/NMF/2014-11-22` will return the
youngest version available which is older than the date specified.
Unfortunately, just as you may not go back in time, you won't be able to query
for versions before the first snapshot date.
