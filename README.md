# Equo Chromium Community Edition

The [Equo Chromium](https://www.equo.dev/chromium) Community widget is a cross platform SWT browser that allows users to create and render modern web-based UIs inside a Java SWT or an Eclipse RCP application. It can be used in a standalone Java application, or it can also be easily embedded in an Eclipse view, using the same API that is used with other SWT browsers.

No more custom code for each platform, no more installations of specific libraries for Linux, no more problems in Windows with IE, no more platform-dependant and browser-specific issues at all. This is a truly **cross platform** SWT Browser that runs seamlessly in all operating systems and in embedded devices.

It is based on and uses the CEF Framework (https://bitbucket.org/chromiumembedded/cef).

## License

[Equo Chromium](https://www.equo.dev/chromium) is dual-licensed under commercial and open source licenses (GPLv3). This repository is licensed under GPLv3. To get a commercial license and to not worry about the obligations associated with the GPLv3 license please contact the [Equo Platform support team](https://www.equo.dev/request-a-demo).

## Distribution

The Equo Chromium SWT Browser is provided as ready-to-use P2 repositories, which contains:

- a Chromium bundle
- a Chromium native library per platform fragments
- a feature containing all the above

CEF Binaries are provided in a separate repository for easy usage.

### P2 repositories

An Eclipse P2 repository is available with the bundle and fragments for Linux, Windows and Mac OS (x86_64 only):

1. Repository Equo Chromium Community widget:

```
https://dl.equo.dev/chromium-swt-ce/69.0.0/repository
```

2. CEF binaries P2 repository:

```
https://dl.equo.dev/chromium-cef-ce/69.0.0/repository
```

If you need support for other platforms (i.e 32 bits architectures) please [contact us](https://www.equo.dev/request-a-demo).

## Usage

To use the Equo Chromium Community widget add the following import sentence to your Java classes:


```java
import com.equo.swt.chromium.Browser;
```

Below you can see an example of how to instantiate the browser in your application:

```java
import com.equo.swt.chromium.Browser;
public class SinglePagePart {
	public void createBrowser(Composite parent, String url) {
		Browser browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		browser.setUrl(url);
	}
}
```

## Contributing

Thanks you to all the people who are contributing to Equo Chromium! Please, read our [Contributors Guide](docs/CONTRIBUTING.md) if you want to contribute to this project.

By contributing you agree to our [Code of Conduct](docs/CODE_OF_CONDUCT.md)'s terms.

## Build

This repo uses GIT LFS, please install (https://git-lfs.github.com/) before cloning.

- Clone this repo.
- `mvn clean package`

### Run tests

- Exract the CEF binaries first or run the widget once to extract the binaries, as indicated in Usage.
- `mvn verify`
- Or from eclipse run the single test class from bundle as Junit tests. (Note: you may need to change paths to .jars in local installation)

Notes: Running with mvn has some test failing due accessing protected fields from same package in different bundes. This is a temporary until it gets merged to SWT.

## Support

If you need consultancy or support for having the widget integrated into your app, contact our [Enterprise support team](mailto:support@equo.dev).
