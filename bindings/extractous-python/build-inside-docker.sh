#!/bin/bash

if [[ $PWD =~ extractous/bindings/extractous-python$ ]]; then
    ROOT_DIR=$(realpath "$PWD/../../")
    # docker build --build-arg HTTP_PROXY="http://172.17.0.1:2080" --build-arg HTTPS_PROXY="http://172.17.0.1:2080" -t manylinux_2_17_graalvm $PWD
    docker build -t manylinux_2_17_graalvm $PWD
    docker run --rm --mount type=bind,source=$ROOT_DIR,target=/workspace manylinux_2_17_graalvm bash /workspace/bindings/extractous-python/build-wheels.sh

    # reset paemissions
    echo ""
    echo "Resettings permissions on some directories that were touched by docker running in root "
    sudo chown -R $USER:$USER $ROOT_DIR
else
    echo "Please make sure to run the script from extractous/bindings/extractous-python"
    exit 1
fi