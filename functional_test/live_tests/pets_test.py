import pytest
from hamcrest import *
from pet_store_client import PetStoreClient

def test_get_pet(pet_context, pet_store_client):
    response = pet_store_client.get_pet(pet_context['id'])

    pet = response.json()
    assert_that(pet['name'], is_('Harry'))
    assert_that(pet['category'], is_('Cat'))
    assert_that(pet['bio'], is_('I am fuzzy'))
    assert_that(pet['id'], is_(pet_context['id']))


def test_list_pets(pet_context, pet_store_client):
    response = pet_store_client.list_pets()

    pets = response.json()
    assert_that(pets, has_length(1))

    assert_that(pets[0]['name'], is_('Harry'))
