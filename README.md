# blobfish

Serve raw content of Git repos over HTTP.

Blobfish is a simple HTTP server which exposes the raw content of the blobs in a Git repo. It is essentially an HTTP wrapper for `git show`.

* It does not support directory indexing and assumes the clients will know the exact paths for the files it wants.
* It *does* support getting files from arbitrary branches.

It also supports configurable redirection to other servers (blobfish or not) for selected branches. This means you can run a blobfish server locally, and have stable URLs for several git branches/repos which are stored and served from other servers.

Although mostly generic, blobfish was primarily designed to serve p2 repositories used as target platforms for Eclipse development. The idea is to store fully resolved Eclipse target platforms (basically sets of JARs) under Git, so that they can be easily versioned, branches, tagged and mirrored, and use blobfish to serve them.

## Installation

1. Install [Clojure](http://clojure.org) and [Leiningen](http://leiningen.org).
2. Clone from http://github.com/pcdavid/blobfish.
3. Run `lein uberjar` from inside the clone.

The resulting `target/blobfish-0.1.0-SNAPSHOT-standalone.jar` is a standalone JAR which can be deployed anywhere and only depends on a JRE 1.5 or later.

## Usage

    $ java -jar blobfish-0.1.0-standalone.jar path/to/config.json

The configuration file is in JSON and has the following format:

    {
        "port": 8080,
        "repo": "/home/pcdavid/repo/.git",
        "remotes": [
            { "custom": "http://blobs.example.com:8081/custom" }
        ]
    }

* The `port` is optional and defaults to 8080.
* The `repo` is mandatory, and should point to the local Git repository to serve. Note that it should refer to the `.git` folder itself, not the root of the working copy.
* The `remotes` is optionals. If present, it must be a vector of simple maps, where the key is a branch name and the value is the root URL to which to redirect requests for that branch.

### Bugs, Limitations and Ideas

* No attention has been paid for now on performance. It has not even been measured. It is probably rather bad.
* blobfish currently invokes an external Git process to serve each request. This has the advantage of simplicity and resource usage isolation, but may cause performance issues (again, not measured yet). We should try to use JGit instead, but not before we have some performance measurements in place. In particular I am not sure how to control JGit's memory usage, especially on repos which can be several gigabytes of binary JARs. Calling an external Git process at least has the benefit that whatever resources Git needs is freed at the end of the request and does not blow up the JVM heap in uncontrolled ways.
* There is no support for `HEAD`, `Content-Length`, `Last-Modified`, or other HTTP verbs and options yet. Some options may be useful to enable caching and should be investigated.
* There is no support for logging.
* Because blobfish serves branches and blobs from their symbolic names (e.g. `master:path/to.jar`), there is a possible race condition if new content is commited on a branch while a client is downloading its content. The main use case (serving p2 repositories, which can be hundreds of megabytes split in hundreds of files and thus individual requests) is particularly exposed to this: if a new version of a p2 repo is commited while a client is loading it, the client will see inconsistencies. p2 repos served by "dumb" HTTP servers using raw files can not do much, but blobfish can: when it gets a request for some special "root" files which identifies the starts of a client session/transaction (e.g `somebranch:p2.index`), it should issue a redirect to that same file through the current id (SHA1) of the branch (e.g. `273cb5985aa0491dcf68dc2af5249d3d29d4b3e1:p2.index`). This way, all the rest of the client requests for that transaction will get a constistent view of the p2 repo from that specific commit, even if `somebranch` moves to something else in the meantime.
* Any behavior specific to the "p2 repository" use case should be only enabled by an explicit flag or configuration option.

## License

Thanks to Obeo for sponsoring this work and allowing me to release it as Open Source/Free Software.

Copyright © 2014 Obeo

Distributed under the Eclipse Public License either version 1.0 or (at your option) any later version.
