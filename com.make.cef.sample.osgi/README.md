## Build

./gradelw build


## Run
./gradlew cleanOsgiRuntime

./gradlew createOsgiRuntime

LD_LIBRARY_PATH=~/workspaces/rust/cefrust/target/debug/

cd build/osgi && chmod +x ./run.sh && ./run.sh && cd ../..

