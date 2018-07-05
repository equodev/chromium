# Chromium for SWT

A SWT widget embedding a cross platform **Chromium** Browser. It works on Windows, Mac OS and Linux.

This new widget allows to develop modern web UIs inside a Java SWT or an Eclipse RCP application. It can be easily embedded in an Eclipse view, using the same API that is used with the other SWT browsers. 

No more custom code for each platform, no more installations of specific libraries for Linux, no more problems in Windows with IE, no more platform-dependant and browser-specific issues at all. This is a truly cross platform SWT Browser that runs seamlessly in all operating systems.

It is based on and uses the CEF Framework (https://bitbucket.org/chromiumembedded/cef).

## P2 repository

An eclipse P2 repository is available with the bundle and fragments for Linux, Windows and Mac OS (x86_64 only)

http://dl.maketechnology.io/chromium.swt/rls/repository

Third party dependencies (JNR, ASM) are available at eclipse repository: http://download.eclipse.org/releases/photon

## Usage

Change your java import to use `import org.eclipse.swt.chromium.Browser;`. It follows the same SWT Browser API. 

	- ⚠️ Some of the APIs are not implemented yet.

    - ⚠️ On linux it works only with GTK2 for now.

	- ⚠️ Make sure you enable the following bundles and their dependencies in your run config:
        - com.github.jnr.ffi
        - com.github.jnr.jffi
        - com.github.jnr.jffi.native (fragment)
        - org.eclipse.swt.chromium
        - org.eclipse.swt.[ws].[os].x86_64 (fragment)

    - ⚠️ You will get an error on first run saying CEF binaries are not available, download cef_binary __3.3071__ client archive from http://opensource.spotify.com/cefbuilds/ and extract binaries to folder indicated in error (~/.swt/lib/*/x86_64/chromium-3071). Then rerun.

	- ⚠️ On Linux, your first launch will fail due to a bug on CEF. You can workaround this by copying `icudtl.dat`, `natives_blob.bin` and `snapshot_blob.bin` from CEF folder to the `jre/bin` folder (e.g.: `/usr/lib/jvm/java-8-oracle/jre/bin/`). Those files should be siblings of the java executable file. Fix is WIP. 

## Build

- Clone this repo.
- `mvn clean package`

## Status and Plan

For now, it just loads and displays a URL, but we have a plan to flesh out the full SWT Browser API, including the Javascript support.

We (**Make Technology**, http://www.maketechnology.io/) have agreed with the **Eclipse Foundation** to do all this work and contribute it under the EPL, but we need your support to make this huge project happen!

https://medium.com/@mikael.barbero/chromium-eclipse-swt-integration-c61f416e97d1

## Donations

Funding this work is welcome. If you think your company is interested in and can help, please contact us (guillez@gmail.com).

## Support

If you need (paid) consultancy or support for having the widget integrated into your app, contact us (Make Technology, http://www.maketechnology.io) at guillez@gmail.com. 

We're offering early support for the widget and consultancy. Note that it will be open source anyway.
