# JWAT-Tools #

JWAT-Tools is an extension to the JWAT utility libraries.

This project currently includes a commandline tool with various gzip/arc/warc/xml tasks.

### Tasks ###

* Arc2Warc
* CDX
* Changed
* Compress
* ContainerMD
* Decompress
* Delete
* Extract
* Interval
* PathIndex
* Test
* Unpack

### History ###

###### V0.6.0 ######

* It is tedious not having deployed this artifact to maven central. So this and future versions will be deployed.
* Fixed serious NPE in PayloadManager for large files.
* Moved some common classes from JWAT-Tools to JWAT. (They will disappear in the new snapshot version)
* Changed to used JWAT-1.0.3

### Maven ###

<dependency>
  <groupId>org.jwat</groupId>
  <artifactId>jwat-tools</artifactId>
  <version>0.6.0</version>
</dependency>

The following 2 artifacts should be executable and with all required dependencies included, when unpacked.

<dependency>
  <groupId>org.jwat</groupId>
  <artifactId>jwat-tools</artifactId>
  <version>0.6.0</version>
  <type>tar.gz</type>
</dependency>

<dependency>
  <groupId>org.jwat</groupId>
  <artifactId>jwat-tools</artifactId>
  <version>0.6.0</version>
  <type>zip</type>
</dependency>
