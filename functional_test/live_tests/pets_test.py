import pytest
from hamcrest import assert_that, is_, has_length
from pet_store_client import PetStoreClient

example_pet = {
    "name": "Harry",
    "category": "Cat",
    "bio": "I am fuzzy",
    "status": "Available",
    "tags": [],
    "photoUrls": []
}


def test_get_pet(pet_context, customer_context, pet_store_client):
    response = pet_store_client.get_pet(pet_context['id'])

    pet = response.json()
    assert_that(pet['name'], is_('Harry'))
    assert_that(pet['category'], is_('Cat'))
    assert_that(pet['bio'], is_('I am fuzzy'))
    assert_that(pet['id'], is_(pet_context['id']))


def test_list_pets_default_pagination(pet_context, customer_context, pet_store_client):
    response = pet_store_client.list_pets()

    pets = response.json()
    assert_that(pets, has_length(1))

    assert_that(pets[0]['name'], is_('Harry'))


def test_list_pets_paginated(pet_context, customer_context, pet_store_client):
    response = pet_store_client.list_pets(pageSize=10,offset=0)
    
    pets = response.json()
    assert_that(pets, has_length(1))

    assert_that(pets[0]['name'], is_('Harry'))


def test_find_pets_by_status(pet_context, customer_context, pet_store_client):

    response = pet_store_client.find_pets_by_status(['Available', 'Pending'])

    pets = response.json()
    assert_that(pets, has_length(1))

    assert_that(pets[0]['name'], is_('Harry'))

def test_find_pets_by_tags(pet_context, customer_context, admin_context, pet_store_client):
    # No Pets with "Amphibian" tags exist yet(
    customer_context()
    response = pet_store_client.find_pets_by_tag(['Amphibian'])
    pets = response.json()
    assert_that(pets, has_length(0))

    # Add a pet
    pet = {
        "name": "Nancy",
        "category": "Frog",
        "bio": "R-r-ribbit!",
        "status": "Pending",
        "tags": ["Green", "Amphibian", "Croaker"],
        "photoUrls": []
    }

    admin_context()
    pet_store_client.create_pet(pet)

    # Grab all pets
    customer_context()
    response = pet_store_client.find_pets_by_tag([''])
    pets = response.json()
    assert_that(pets, has_length(2))
    assert_that(pets[0]['name'], is_('Harry'))

    # Retry "Amphibian" tag - there should be exactly one now
    response = pet_store_client.find_pets_by_tag(['Amphibian'])
    pets = response.json()
    assert_that(pets, has_length(1))
    assert_that(pets[0]['name'],is_('Nancy'))

def test_update_pet(pet_store_client, admin_context):
    pet = {
        "name": "JohnnyUpdate",
        "category": "Cat",
        "bio": "I am fuzzy",
        "status": "Available",
        "tags": ["Cat","Fuzzy","Fur ball"],
        "photoUrls": []
    }

    saved_pet = None

    try:
        response = pet_store_client.create_pet(pet)
        saved_pet = response.json()
        saved_pet['bio'] = "Not so fuzzy"

        response = pet_store_client.update_pet(saved_pet)
        updated_pet = response.json()

        assert_that(updated_pet['bio'], is_('Not so fuzzy'))
    finally:
        if saved_pet:
            admin_context()
            pet_store_client.delete_pet(saved_pet['id'])


def test_update_pet_not_found(pet_store_client, admin_context):
    pet = dict(example_pet, **{"id": 9999999})
    response = pet_store_client.update_pet(pet)
    assert_that(response.status_code, is_(404))

def test_only_admin_can_update_pet(pet_store_client, customer_context, admin_context):
    new_pet = dict(example_pet, **{"name": "test_customer_cannot_update"})
    admin_context()
    saved_pet_resp = pet_store_client.create_pet(new_pet)
    saved_pet = saved_pet_resp.json()
    customer_context()
    updated_pet = dict(saved_pet, **{"status": "Adopted"})
    bad_update = pet_store_client.update_pet(updated_pet)
    assert_that(bad_update.status_code, is_(401))
    admin_context()
    good_update = pet_store_client.update_pet(updated_pet)
    assert_that(good_update.status_code, is_(200))
    pet_store_client.delete_pet(saved_pet)

def test_only_admin_can_delete_pet(pet_store_client, customer_context, admin_context):
    new_pet = dict(example_pet, **{"name": "test_customer_cannot_delete"})
    admin_context()
    saved_pet_resp = pet_store_client.create_pet(new_pet)
    saved_pet = saved_pet_resp.json()
    customer_context()
    bad_delete = pet_store_client.delete_pet(saved_pet['id'])
    assert_that(bad_delete.status_code, is_(401))
    admin_context()
    good_delete = pet_store_client.delete_pet(saved_pet['id'])
    assert_that(good_delete.status_code, is_(200))
