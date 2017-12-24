import pytest
from hamcrest import *
from pet_store_client import PetStoreClient

def test_signup(pet_store_client):
    user = {
        "userName": "jwick200",
        "firstName": "John",
        "lastName": "Wick",
        "email": "wheresmycar@gmail.com",
        "password": "wickofyourwit",
        "phone": "215-789-0123"
    }
    response = pet_store_client.signup_user(user)

    assert_that(response.status_code, is_(200))
    new_user = response.json()
    assert_that(new_user['id'], not_none())

def test_signup_existing_user(pet_store_client):
    user = {
        "userName": "jwick201",
        "firstName": "John",
        "lastName": "Wick",
        "email": "wheresmycar@gmail.com",
        "password": "wickofyourwit",
        "phone": "215-789-0123"
    }
    response = pet_store_client.signup_user(user)

    assert_that(response.status_code, is_(200))
    new_user = response.json()
    assert_that(new_user['id'], not_none())

    # Attempt to create a user with the same name results in a conflict
    response = pet_store_client.signup_user(user)
    assert_that(response.status_code, is_(409))

