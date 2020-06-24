# A local proxy for Forex rates challenge

### Build a local proxy for getting Currency Exchange Rates

See [details](https://github.com/paidy/interview/blob/master/Forex.md "Details")

### Restrictions
1. We limited only to 1000 requests per day.
2. We have 72 unique pairs of courencies. Rates for them should be at max 5 minutes old.
3. Our service should handle at least 10000 request per day.

### Solution
Instead of calling One Frame API for each pair of currencies separately we will use scheduler and download rates for all 72 pairs for one call. In that way if we for example will be calling One Frame api once per 4 minutes we will consume only 360 request in 24 hours. Results of our calls we will keep in cache for 5 minutes to be able to serve our clients.

### How to run
One frame API token from [this](https://hub.docker.com/r/paidyinc/one-frame) page can be assigned to environment variable `ONEFRAME_AUTH_TOKEN` and then it's possible to run project with usual `sbt run` command or passed token to sbt like this: `sbt '-J-DONEFRAME_AUTH_TOKEN=your-token' run`  
Server will listen on localhost on port 8080
