#!/usr/bin/env bash
# set -euo pipefail

(
    # Check if two arguments (username and password) are provided
    if [ "$#" -ne 2 ]; then
        echo "Usage: $0 <username> <password>"
        exit 1
    fi

    USERNAME=$1
    PASSWORD=$2


    cd $KOTO_REKISTERI/scripts
    (rm -rf TEMP) && mkdir -p TEMP
    cd TEMP

    # Step 1: Log in to /auth/login with hardcoded credentials
    # for some reason curl wants -w i don't know why.
    RESPONSE=$(curl -s -i \
        -X POST "https://virkailija.testiopintopolku.fi/cas/v1/tickets" \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "username=$USERNAME&password=$PASSWORD")
    
    echo $RESPONSE > RESPONSE.txt

    STATUS=$(echo $RESPONSE | grep HTTP | awk '{ print $2 }') 
    if [ $STATUS -ne 201 ]; then 
        echo "login failed"
        exit 1
    fi
    
    # TODO: Fix this. headers is split with \r
    LOCATION=$(echo $RESPONSE | grep location )  
    echo "Location:'$LOCATION'"

    # Make the HTTP POST request
    # curl -s -w "%{http_code}" -o /dev/null -X POST "$URL" \
    #     -H "Caller-Id: " \
    #     -H "CSRF: CSRF" \
    #     -H "Cookie: CSRF=CSRF" \
    #     -H "Content-Type: application/json"
)