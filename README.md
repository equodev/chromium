# Chromium for SWT

A SWT widget embedding a cross platform **Chromium** Browser. It works on Windows, Mac OS and Linux.

This new widget allows to develop modern web UIs inside a Java SWT or an Eclipse RCP application. It can be easily embedded in an Eclipse view, using the same API that is used with the other SWT browsers. 

No more custom code for each platform, no more installations of specific libraries for Linux, no more problems in Windows with IE, no more platform-dependant and browser-specific issues at all. This is a truly cross platform SWT Browser that runs seamlessly in all operating systems.

It is based on and uses the CEF Framework (https://bitbucket.org/chromiumembedded/cef).

## Design

Chromium SWT Browser is provided as a P2 repository ready to use, which contains:

- chromium bundle
- chromium native library per platform fragments
- feature containing all the above

CEF Binaries are provided in a separate repository for easy usage.

## P2 repository

An eclipse P2 repository is available with the bundle and fragments for Linux, Windows and Mac OS (x86_64 only)

1- Chromium Browser Widget repository (EPL): 
  http://dl.maketechnology.io/chromium-swt/rls/repository

2- ~~Third party dependencies (JNR, ASM) are available at eclipse repository (EPL)~~ (No dependencies since 0.10.0):
  http://download.eclipse.org/releases/photon

3- CEF Binaries (Optional) required to run can be manually copied to ~/.swt/lib/[os]/x86_64/ or you can use this repository which provides a feature and per platform fragments with CEF binaries and resources.
  http://dl.maketechnology.io/chromium-cef/rls/repository

## Usage

Change your java import to use `import org.eclipse.swt.chromium.Browser;`. It follows the same SWT Browser API.

Notes:

  - ~~On linux it works only with GTK2 for now~~. Since 0.9.0 it works with GTK3, with CEF binaries repository which has some patches on CEF for gtk3.

	- Make sure you enable the following bundles and their dependencies in your run config:
        - ~~com.github.jnr.ffi~~
        - ~~com.github.jnr.jffi~~
        - ~~com.github.jnr.jffi.native (fragment)~~
        - org.eclipse.swt.chromium
        - org.eclipse.swt.[ws].[os].x86_64 (fragment)

    - If you don't use CEF Binaries repository, you will get an error on first run saying CEF binaries are not available. Use the CEF Binaries repository or manually download cef_binary __3.3071__ client archive from http://opensource.spotify.com/cefbuilds/ and extract binaries to folder indicated in error (~/.swt/lib/*/x86_64/chromium-3071). Then rerun.

	- ⚠️ On Linux, your first launch may fail due to a bug on CEF. You can workaround this by copying `icudtl.dat`, `natives_blob.bin` and `snapshot_blob.bin` from CEF folder to the `jre/bin` folder (e.g.: `/usr/lib/jvm/java-8-oracle/jre/bin/`). Those files should be siblings of the java executable file. Or use our CEF binaries repo which contains the fix.

### Run SWT Browser Example

- On Eclipse Photon, go to Welcome > Samples > SWT > Workbench views and standalone applications (install if asked to) > Finish wizard.
- Install chromium feature from http://dl.maketechnology.io/chromium-swt/rls/repository
- Install cef feature from http://dl.maketechnology.io/chromium-cef/rls/repository
- Add `org.eclipse.swt.chromium` plugin dependency to _/org.eclipse.swt.examples/META-INF/MANIFEST.MF_ 
- Open and modify `BrowserExample` class:
  - Add `import import org.eclipse.swt.chromium.Browser;`
  - Use FQNs `org.eclipse.swt.chromium.OpenWindowListener` and `org.eclipse.swt.chromium.WindowEvent` in lines 62 and 63. 
- "run the sample" from the link in SWT Examples welcome view.
- Once eclipse opens, go to Show View > Other > SWT Examples > Web Browser

## Build

This repo uses GIT LFS, please install (https://git-lfs.github.com/) before cloning.

- Clone this repo.
- `mvn clean package`

### Run tests

- Exract the CEF binaries first or run the widget once to extract the binaries, as indicated in Usage.
- `mvn verify`
- Or from eclipse run the single test class from bundle as Junit tests. (Note: you may need to change paths to .jars in local installation)

Notes: Running with mvn has some test failing due accessing protected fields from same package in different bundes. This is a temporary until it gets merged to SWT.


## Status and Plan

The full SWT Browser API is supported now, including sync and async Javascript support.

We (https://www.equoplatform.com, former **Make Technology**) agreed with the **Eclipse Foundation** to do all this work and we have contributed it under the EPL. However, we need your support to continue contributing features and bug fixes.

https://medium.com/@mikael.barbero/chromium-eclipse-swt-integration-c61f416e97d1

## Donations

Funding this work is welcome. If you think your company is interested in and can help, please contact us (contact@equoplatform.com).

## New Versions and Support

For newer Chromium versions, frequent updates, critical fixes, enterprise support, and more, please check out https://equoplatform.com/#/pricing.
