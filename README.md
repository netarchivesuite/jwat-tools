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

###### V0.6.3-SNAPSHOT ######

* Added verify code to ARC/WARC compress task.
* Compress file writes old/new filename/length/md5 to system.out for now.

###### V0.6.2 ######

* Updated dependency to JWAT-1.0.4.

###### V0.6.1 ######

* Thomas LEDOUX: ContainerMDUtils class - function encodeContent() replaced by new function which is faster (about 10 times) and takes care of all the protected characters (even the control ones).
* Added extract code for WARC and GZip. Unit tests to follow soon.
* Added version to manifest. Show version in manifest instead of a constant string.
* Removed some JVM options in the start scripts.
* Moved common classes from JWAT-Tools to JWAT.

###### V0.6.0 ######

* It is tedious not having deployed this artifact to maven central. So this and future versions will be deployed.
* Fixed serious NPE in PayloadManager for large files.
* Moved some common classes from JWAT-Tools to JWAT. (They will disappear in the new snapshot version)
* Changed to used JWAT-1.0.3

### Maven ###

jar dependency.

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

or

        <dependency>
            <groupId>org.jwat</groupId>
            <artifactId>jwat-tools</artifactId>
            <version>0.6.0</version>
            <type>zip</type>
        </dependency>