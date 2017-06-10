# Chromium for SWT

This is a SWT widget embedding **Chromium** Browser for Windows, Mac OS and Linux.

It uses CEF Framework (https://bitbucket.org/chromiumembedded/cef)

## Sample application 

For now you can get a sample application ready to run. Get the zip for your platform, unzip it and run the cef_rcp app.

- [Download](https://github.com/maketechnology/cefswt/releases/download/v0.1-alpha/cef_rcp-win32.x86_64.zip) Demo for Windows
- [Download](https://github.com/maketechnology/cefswt/releases/download/v0.1-alpha/cef_rcp-macosx.x86_64.zip) Demo for Mac
- [Download](https://github.com/maketechnology/cefswt/releases/download/v0.1-alpha/cef_rcp-linux.x86_64.zip) Demo for Linux
  - ⚠️ On Linux, your first lauch will fail due a bug on CEF. You can workaround this by copying `icudtl.dat`, `natives_blob.bin` and `snapshot_blob.bin` from `~/..swtcef/3.3029.1611.g44e39a8/linux-x86_64`folder to jre/bin folder (e.g.: `/usr/lib/jvm/java-8-oracle/jre/bin/`). Those files should be siblings of the java executable file.

## Status and Plan

For now it just display a URL, but we a have a plan to flesh out the full SWT Browser API, including javascript support.

We (**Make Technology**, http://www.wemaketechnology.com/) have agreed to do all this work and contribute it to the **Eclipse Foundation** under the EPL, but we need your support!

## Donations

We need funding to complete this work and **Eclipse Foundation** is accepting donations to make it happen as part of Eclipse and EPL. If you think your company is interested and can help, contact us (guillez at gmail) and we will put you in contact with _Eclipse Foundation_.
