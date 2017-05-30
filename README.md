# Sample Eclipse E4 app usin CEF SWT build with mvn/tycho

Uses maven wrapper, no need to install maven

## Build

./mvnw clean verify

## Run sample app

unzip com.make.cef.sample.e4.rcp/target/products/cef_rcp-*.zip
run cef_rcp.exe


# Generate java cef bindings

clone git@github.com:maketechnology/ffi_gen.git

```
gem build ffi_gen.gemspec
sudo gem install ./ffi_gen-1.2.0.gem 
```

clone git@github.com:maketechnology/cef.java.binding.git
```
ln -s $PWD/../make.cefswt/build/cef_binary_3.3029.1611.g44e39a8_linux64_minimal/include/base cef/include/base
ln -s $PWD/../make.cefswt/build/cef_binary_3.3029.1611.g44e39a8_linux64_minimal/include/capi cef/include/capi
ln -s $PWD/../make.cefswt/build/cef_binary_3.3029.1611.g44e39a8_linux64_minimal/include/internal cef/include/internal
rake
```

# Notes by Emiliano
1. Install Rust (https://www.rust-lang.org)
2. Clone https://github.com/maketechnology/cefswt
3. Clone https://github.com/maketechnology/cefrust
4. Switch to 3029 branch in both repositories.
5. Open cdm.
6. Go to where gradlew.bat is (https://github.com/maketechnology/cefswt/gradlew.bat)
7. For build execute: gradlew.bat copyCef buildSampleE4
8. Go to ...\cefswt\com.make.cef.sample.e4.rcp\target\products\
9. Unzip produc and execute cef_rcp.exe: cef_rcp.exe -consoleLog
10. Download jre https://gitlab.com/maketechnology/jres/builds/12670474/artifacts/download
11. Paste into the unzipped directory.
