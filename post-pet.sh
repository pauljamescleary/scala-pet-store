#!/bin/bash

curl -vX POST http://localhost:8080/pet -d @pet.json --header "Content-Type: application/json"
