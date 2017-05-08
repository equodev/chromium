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