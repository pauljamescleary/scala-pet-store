import pytest
from hamcrest import *
from pet_store_client import PetStoreClient


def test_place_order(pet_context, pet_store_client):
    order = {
        "petId": pet_context['id'],
        "status": "Placed",
        "complete": False
    }
    response = pet_store_client.place_order(order)

    order = response.json()
    assert_that(order['status'], is_('Placed'))
    assert_that(order['complete'], is_(False))
    assert_that(order['id'], is_(not_none()))
    assert_that(order['shipDate'], is_(none()))

def test_get_order(pet_context, pet_store_client):
    order = {
        "petId": pet_context['id'],
        "status": "Placed",
        "complete": False
    }
    response = pet_store_client.place_order(order)

    placed_order = response.json()
    order_id = placed_order['id']

    response = pet_store_client.get_order(order_id)
    order = response.json()
    assert_that(order, is_(placed_order))

def test_delete_order(pet_context, pet_store_client):
    order = {
        "petId": pet_context['id'],
        "status": "Placed",
        "complete": False
    }
    response = pet_store_client.place_order(order)

    placed_order = response.json()
    order_id = placed_order['id']

    response = pet_store_client.delete_order(order_id)
    assert_that(response.status_code, is_(200))
