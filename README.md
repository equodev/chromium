# Equo Chromium Community Edition (**V106**)

The [Equo Chromium](https://www.equo.dev/chromium) Community widget is a cross platform browser that allows users to create and render modern web-based UIs inside a Java application. It can be used in standalone Java applications, Windowless, Swing(coming soon), SWT, or Eclipse RCP applications.

No more custom code for each platform, no more installations of specific libraries for Linux, no more problems in Windows with IE, no more platform-dependant and browser-specific issues at all. This is a truly **cross platform** browser that runs seamlessly in all operating systems and in embedded devices.

It is based on and uses the CEF Framework (https://bitbucket.org/chromiumembedded/cef).

## Usage

To use the Equo Chromium Community widget add the following import sentence to your Java classes:


```java
import com.equo.chromium.swt.Browser;
```

Below you can see an example of how to instantiate the browser in your application:

```java
import com.equo.chromium.swt.Browser
public class SinglePagePart {
	public void createBrowser(Composite parent, String url) {
		Browser browser = new Browser(parent, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		browser.setUrl(url);
	}
}
```

## Examples

Complete Sample projects can be found [here](https://github.com/equoplatform/chromium-samples).

## Distribution

The Equo Chromium Browser is provided as ready-to-use P2, MVN and OSGi repositories, which contains:

- a Chromium bundle
- a Chromium native library per platform fragments
- a feature containing all the above

### Repositories

Repositories are available for Linux, Windows and macOS (x86_64 only)

[Equo Chromium repositories](https://dl.equo.dev/chromium-swt-ce/oss/mvn/index.html)

If you need support for other platforms (i.e 32 bits or ARM architectures) please [contact us](https://www.equo.dev/request-a-demo).

## Equo documentation

https://docs.equo.dev/main/getting-started/introduction.html

## Contributing

Thanks you to all the people who are contributing to Equo Chromium! Please, read our [Contributors Guide](docs/CONTRIBUTING.md) if you want to contribute to this project.

By contributing you agree to our [Code of Conduct](docs/CODE_OF_CONDUCT.md)'s terms.

## Build

This repo uses GIT LFS, please install (https://git-lfs.github.com/) before cloning.

- Clone this repo.
- `mvn clean package`


## License

[Equo Chromium](https://www.equo.dev/chromium) is dual-licensed under commercial and open source licenses (GPLv3). This repository is licensed under GPLv3. To get a commercial license and to not worry about the obligations associated with the GPLv3 license please contact the [Equo support team](https://www.equo.dev/request-a-demo).

## [Release Notes](https://docs.equo.dev/chromium/ce-106.x/reference/release-notes.html)

## Support

If you need consultancy or support for having the widget integrated into your app, contact our [Enterprise support team](mailto:support@equo.dev).
