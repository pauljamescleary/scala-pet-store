import pytest
from pet_store_client import PetStoreClient

example_user = {
    "userName": "example",
    "firstName": "John",
    "lastName": "Wick",
    "email": "wheresmycar@gmail.com",
    "password": "wickofyourwit",
    "phone": "215-789-0123",
    "role": "Admin"
}

pet = {
    "name": "Harry",
    "category": "Cat",
    "bio": "I am fuzzy",
    "status": "Available",
    "tags": [],
    "photoUrls": []
}

def dict_filter(u, *ks):
    return {k: v for k, v in list(u.items()) if k in ks}

def login_from_user(u):
    return dict_filter(u, "userName", "password")

@pytest.fixture(scope="session")
def pet_store_client(request):
    return PetStoreClient()

def user_context(request, pet_store_client, userName, role):
    user1 = dict(example_user, **{"userName": userName, "role": role})
    
    pet_store_client.signup_user(user1)
    pet_store_client.login_user(login_from_user(user1))

    request.addfinalizer(lambda : pet_store_client.delete_user_by_username(userName))
    
    #return here as a lambda, in case we need to reauthenticate a user, 
    #for example to switch between customer and admin roles
    return lambda: pet_store_client.login_user(login_from_user(user1))

@pytest.fixture(scope="session")
def pet_context(request, pet_store_client):
    user_context(request, pet_store_client, "pet_breeder", "Admin")

    response = pet_store_client.create_pet(pet)
    saved_pet = response.json()

    request.addfinalizer(lambda: pet_store_client.delete_pet(saved_pet['id']))

    return saved_pet

@pytest.fixture(scope="function")
def admin_context(request, pet_store_client): 
    return user_context(request, pet_store_client, "pet_breeder_fun", "Admin")

@pytest.fixture(scope="function")
def customer_context(request, pet_store_client):
    return user_context(request, pet_store_client, "pet_lover_fun", "Customer")
