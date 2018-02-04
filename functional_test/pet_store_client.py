import json
import requests
import urllib
from requests.adapters import HTTPAdapter
from requests.packages.urllib3.util.retry import Retry

from urlparse import urljoin
from urlparse import urlparse
from urlparse import parse_qs
from urlparse import urlsplit

class PetStoreClient(object):
    def __init__(self, url='http://localhost:8080'):
        self.index_url = url
        self.headers = {
            'Accept': 'application/json, text/plain',
            'Content-Type': 'application/json'
        }

        self.session = requests.Session()

    def make_request(self, url, method='GET', headers={}, body_string=None, **kw):

        response = self.session.request(method, url, data=body_string, headers=headers, **kw)
        return response.status_code, response.json()

    def create_pet(self, pet):
        """
        Creates a pet, returning the created pet response
        :param pet:
        :return:
        """
        url = urljoin(self.index_url, '/pets')

        return self.session.request('POST', url, self.headers, json.dumps(pet))

    def update_pet(self, pet):
        """
        Updates a pet, returning the updated pet response
        :param pet:
        :return:
        """
        url = urljoin(self.index_url, "/pets/{0}".format(pet['id']))

        return self.session.request('PUT', url, self.headers, json.dumps(pet))

    def get_pet(self, pet_id):
        """
        Returns the pet for the given id
        :param pet_id:
        :return:
        """
        url = urljoin(self.index_url, "/pets/{0}".format(pet_id))

        return self.session.request('GET', url, self.headers)

    def list_pets(self, page_size=10, offset=0):
        """
        Returns a list of pets
        :return:
        """
        url = urljoin(self.index_url, "/pets?pageSize={0}&offset={1}".format(page_size, offset))

        return self.session.request('GET', url, self.headers)

    def find_pets_by_status(self, statuses):
        """
        Returns a list of pets that match the statuses provided
        :param statuses: A list of valid Pet statuses
        :return:
        """
        status_kv_pairs = map(lambda x: "status={0}".format(x), statuses)
        params =  "&".join(status_kv_pairs)
        url = urljoin(self.index_url, "/pets/findByStatus?{0}".format(params))
        return self.session.request('GET', url, self.headers)

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
        tags_kv_pairs = map(lambda x: "tags={0}".format(x), tags)
        params = "&".join(tags_kv_pairs)
        url = urljoin(self.index_url, "/pets/findByTags?{0}".format(params))
        return self.session.request('GET', url, self.headers)

    def delete_pet(self, pet_id):
        """
        Deletes a pet
        :return:
        """
        url = urljoin(self.index_url, "/pets/{0}".format(pet_id))

        return self.session.request('DELETE', url, self.headers)

    def place_order(self, order):
        """
        Places an order for a pet
        """
        url = urljoin(self.index_url, '/orders')

        return self.session.request('POST', url, self.headers, json.dumps(order))

    def get_order(self, order_id):
        """
        Gets an order by id
        """
        url = urljoin(self.index_url, "/orders/{0}".format(order_id))

        return self.session.request('GET', url, self.headers)

    def delete_order(self, order_id):
        """
        Deletes an order
        """
        url = urljoin(self.index_url, "/orders/{0}".format(order_id))

        return self.session.request('DELETE', url, self.headers)

    def signup_user(self, user):
        """
        Signs up a new user
        """
        url = urljoin(self.index_url, '/users')

        return self.session.request('POST', url, self.headers, json.dumps(user))

    def login_user(self, user):
        """
        Logs in a new user
        """
        url = urljoin(self.index_url, '/login')

        return self.session.request('POST', url, self.headers, json.dumps(user))

    def update_user(self, user):
        """
        Updates a user, returning the updated user response
        :param user:
        :return:
        """
        url = urljoin(self.index_url, "/users/{0}".format(user['userName']))

        return self.session.request('PUT', url, self.headers, json.dumps(user))

    def list_users(self, page_size=10, offset=0):
        """
        Returns a list of users
        :return:
        """
        url = urljoin(self.index_url, "/users?pageSize={0}&offset={1}".format(page_size, offset))

        return self.session.request('GET', url, self.headers)


    def find_user_by_name(self, userName):
        """
        Get user by userName
        """
        url = urljoin(self.index_url, "/users/{0}".format(userName))

        return self.session.request('GET', url, self.headers)

    def delete_user_by_username(self, userName):
        """
        Delete user by userName
        """
        url = urljoin(self.index_url, "/users/{0}".format(userName))

        return self.session.request('DELETE', url, self.headers)
