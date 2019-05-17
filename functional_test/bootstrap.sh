#!/bin/bash -e

if [ ! -d "./.virtualenv" ]; then
    echo "Creating virtualenv..."
    virtualenv --clear --python="$(which python3)" ./.virtualenv
fi

if ! diff ./requirements.txt ./.virtualenv/requirements.txt &> /dev/null; then

     echo "Installing dependencies..."
     ./.virtualenv/bin/python3 ./.virtualenv/bin/pip3 install --index-url https://pypi.python.org/simple/ -r ./requirements.txt

     cp ./requirements.txt ./.virtualenv/
fi
