# Chromium for SWT

A SWT widget embedding a cross platform **Chromium** Browser. It works on Windows, Mac OS and Linux.

This new widget allows to develop modern web UIs inside a Java SWT or an Eclipse RCP application. It can be easily embedded in an Eclipse view, using the same API that is used with the other SWT browsers. 

No more custom code for each platform, no more installations of specific libraries for Linux, no more problems in Windows with IE, no more platform-dependant and browser-specific issues at all. This is a truly cross platform SWT Browser that runs seamlessly in all operating systems.

It is based on and uses the CEF Framework (https://bitbucket.org/chromiumembedded/cef).

## Sample application 

For the time being you can get a sample application ready to run. Get the zip for your platform, unzip it and run the cef_rcp app.

- [Download Demo from Release page](https://github.com/maketechnology/cefswt/releases)
 
	- ⚠️ On Linux, your first launch will fail due to a bug on CEF. You can workaround this by copying `icudtl.dat`, `natives_blob.bin` and `snapshot_blob.bin` from `~/.swtcef/3.3029.1611.g44e39a8/linux-x86_64`folder to the `jre/bin` folder (e.g.: `/usr/lib/jvm/java-8-oracle/jre/bin/`). Those files should be siblings of the java executable file.

	- This [link](https://bitbucket.org/chromiumembedded/cef/issues/1936/override-paths-dir_exe-dir_module-on-linux) describes the issue in more detail. There are also two PRs opened that require some effort to be merged and solve the problem.

## Status and Plan

For now, it just loads and displays a URL, but we have a plan to flesh out the full SWT Browser API, including the Javascript support.

We (**Make Technology**, http://www.wemaketechnology.com/) have agreed with the **Eclipse Foundation** to do all this work and contribute it under the EPL, but we need your support to make this huge project happen!

https://medium.com/@mikael.barbero/chromium-eclipse-swt-integration-c61f416e97d1

## Donations

We need funding to complete this work and the **Eclipse Foundation** is accepting donations to make it happen as part of Eclipse and with the EPL. If you think your company is interested in and can help, please contact us (guillez@gmail.com).
