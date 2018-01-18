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

def test_list_users(pet_context, pet_store_client):
    response = pet_store_client.list_users()

    users = response.json()

    assert_that(users[0]['firstName'], is_('John'))

def test_update_user(pet_context, pet_store_client):
    user = {
        "userName": "jwick201",
        "firstName": "John",
        "lastName": "Wicked",
        "email": "wheresmycar@gmail.com",
        "password": "wickofyourwit",
        "phone": "215-789-0123",
        "id": 1
    }
    response = pet_store_client.update_user(user)
    assert_that(response.status_code, is_(200))

    updated_user = response.json()
    assert_that(updated_user['lastName'], is_('Wicked'))
