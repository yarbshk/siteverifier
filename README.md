# SiteVerifier

> NOTE: Before you begin, make sure `in-sites.txt` is created and contains a source list of URLs.

Getting started:
```bash
./gradlew clean build
java -jar ./build/libs/siteverifier-1.0.2.jar in-sites.txt out-sites.txt 2000000
```