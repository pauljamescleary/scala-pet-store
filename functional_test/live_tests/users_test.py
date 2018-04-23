import pytest
from hamcrest import *
from pet_store_client import PetStoreClient

user1 = {
    "userName": "jwick200",
    "firstName": "John",
    "lastName": "Wick",
    "email": "wheresmycar@gmail.com",
    "password": "wickofyourwit",
    "phone": "215-789-0123"
}

user2 = dict(user1, **{"userName": "jwick201"})

def test_signup(pet_store_client):
    response = pet_store_client.signup_user(user1)

    assert_that(response.status_code, is_(200))
    new_user = response.json()
    assert_that(new_user['id'], not_none())

def test_login(pet_store_client):
    response = pet_store_client.login_user(user1)

    assert_that(response.status_code, is_(200))
    new_user = response.json()
    assert_that(new_user['id'], not_none())
    assert_that(new_user['userName'], is_(user1['userName']))

    incorrect_password = dict(user1, **{'password': 'incorrect_password'});
    bad_pass_resp = pet_store_client.login_user(incorrect_password);
    assert_that(bad_pass_resp.status_code, is_(400))

def test_signup_existing_user(pet_store_client):
    response = pet_store_client.signup_user(user2)

    assert_that(response.status_code, is_(200))
    new_user = response.json()
    assert_that(new_user['id'], not_none())

    # Attempt to create a user with the same name results in a conflict
    response = pet_store_client.signup_user(user2)
    assert_that(response.status_code, is_(409))

def test_user_by_username(pet_store_client):
    response = pet_store_client.find_user_by_name(user2['userName'])

    resp_user = response.json()

    assert_that(resp_user['userName'], is_(user2['userName']))

def test_list_users(pet_context, pet_store_client):
    response = pet_store_client.list_users()

    users = response.json()

    assert_that(users[0]['firstName'], is_('John'))

def test_update_user(pet_context, pet_store_client):
    new_last_name = "Wicked"
    response_lookup = pet_store_client.find_user_by_name(user2['userName'])

    resp_user = response_lookup.json()

    update_user = dict(resp_user, **{"lastName" : new_last_name})

    response_update = pet_store_client.update_user(update_user)
    assert_that(response_update.status_code, is_(200))

    response_lookup2 = pet_store_client.find_user_by_name(user2['userName'])
    post_update_user = response_lookup2.json()

    assert_that(post_update_user['lastName'], is_(new_last_name))

def test_invalid_update_user(pet_context, pet_store_client):
    new_last_name = "ThisWon'tWork"

    lookup = pet_store_client.find_user_by_name(user2['userName'])
    lookup_user = lookup.json()

    # Try to change the id in the user to an id that cannot exist in the database
    update_user = dict(lookup_user, **{"lastName": new_last_name, "id": -1})

    response_update = pet_store_client.update_user(update_user)
    assert_that(response_update.status_code, is_(404))

    lookup2 = pet_store_client.find_user_by_name(user2['userName'])
    lookup2_user = lookup2.json()

    assert_that(lookup2_user['userName'], is_(user2['userName']))


def test_delete_user_by_username(pet_store_client):
    response = pet_store_client.delete_user_by_username(user1['userName'])

    assert_that(response.status_code, is_(200))

