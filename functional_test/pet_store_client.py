import json
import requests

from urllib.parse import urljoin
from urllib.parse import urlparse
from urllib.parse import parse_qs
from urllib.parse import urlsplit

class PetStoreClient(object):
    def __init__(self, url='http://localhost:8080'):
        self.index_url = url
        self.headers = {
            'Accept': 'application/json, text/plain',
            'Content-Type': 'application/json'
        }

        self.authHeader = None
        self.session = requests.Session()
    
    def url(self, path):
        return urljoin(self.index_url, path)

    def make_request(self, path, ob=None, method='GET', headers={}, **kw):
        hs = dict(dict(self.headers, **headers), **{"Authorization": self.authHeader})
        u = self.url(path)
        response = None
        if ob is None:
            response = self.session.request(method, u, headers=hs, **kw)
        else:
            response = self.session.request(method, u, headers=hs, data=json.dumps(ob), **kw)
        if "Authorization" in response.headers:
            self.authHeader = response.headers['Authorization']
        return response

    def create_pet(self, pet):
        """
        Creates a pet, returning the created pet response
        :param pet:
        :return:
        """
        return self.make_request('/pets', pet, method='POST')

    def update_pet(self, pet):
        """
        Updates a pet, returning the updated pet response
        :param pet:
        :return:
        """        
        return self.make_request("/pets/{0}".format(pet['id']), pet, 'PUT')

    def get_pet(self, pet_id):
        """
        Returns the pet for the given id
        :param pet_id:
        :return:
        """
        return self.make_request("/pets/{0}".format(pet_id))

    def list_pets(self, **kwargs):
        """
        Returns a list of pets, taking optional keyword argument page_size and offset for pagination
        :return:
        """
        return self.make_request("/pets", params=kwargs)

    def find_pets_by_status(self, statuses):
        """
        Returns a list of pets that match the statuses provided
        :param statuses: A list of valid Pet statuses
        :return:
        """
        status_kv_pairs = ["status={0}".format(x) for x in statuses]
        params =  "&".join(status_kv_pairs)
        return self.make_request("/pets/findByStatus?{0}".format(params))

    def find_pets_by_tag(self, tags):
        """
        Returns a list of pets which match the tags provided.

        Note: The current implementation of Pet stores the tags in a comma-delimited string. An inherent
        shortcoming of how tags are stored means that: 1. the most accurate method of finding matches is through using
        the LIKE operator, and 2. an unintended result is that an input containing substrings and commas can pass, despite
        not being a full tag itself. (eg. given tags "foo" and "bar" to create tag string "foo,bar", "o,b" would pass)

        :param tags: A list of valid tags.
        :return:
        """
        tags_kv_pairs = ["tags={0}".format(x) for x in tags]
        params = "&".join(tags_kv_pairs)
        return self.make_request("/pets/findByTags?{0}".format(params))

    def delete_pet(self, pet_id):
        """
        Deletes a pet
        :return:
        """
        return self.make_request("/pets/{0}".format(pet_id), method='DELETE')

    def place_order(self, order):
        """
        Places an order for a pet
        """
        return self.make_request('/orders', order, method='POST')

    def get_order(self, order_id):
        """
        Gets an order by id
        """
        return self.make_request("/orders/{0}".format(order_id))

    def delete_order(self, order_id):
        """
        Deletes an order
        """
        return self.make_request("/orders/{0}".format(order_id), method='DELETE')

    def signup_user(self, user):
        """
        Signs up a new user
        """
        return self.make_request('/users', user, 'POST')

    def login_user(self, user):
        """
        Logs in a new user
        """
        return self.make_request('/users/login', user, 'POST')

    def update_user(self, user):
        """
        Updates a user, returning the updated user response
        :param user:
        :return:
        """
        return self.make_request("/users/{0}".format(user['userName']), user, 'PUT')

    def list_users(self, **kwargs):
        """
        Returns a list of users
        :return:
        """
        return self.make_request("/users", params=kwargs)

    def find_user_by_name(self, userName):
        """
        Get user by userName
        """
        return self.make_request("/users/{0}".format(userName))

    def delete_user_by_username(self, userName):
        """
        Delete user by userName
        """
        return self.make_request("/users/{0}".format(userName), method='DELETE')
