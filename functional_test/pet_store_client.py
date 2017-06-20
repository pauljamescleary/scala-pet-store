import json
import requests
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

    def get_pet(self, pet_id):
        """
        Returns the pet for the given id
        :param pet_id:
        :return:
        """
        url = urljoin(self.index_url, "/pets?id={0}".format(pet_id))

        return self.session.request('GET', url, self.headers)

    def list_pets(self, page_size=10, offset=0):
        """
        Returns a list of pets
        :return:
        """
        url = urljoin(self.index_url, "/pets?pageSize={0}&offset={1}".format(page_size, offset))

        return self.session.request('GET', url, self.headers)

    def delete_pet(self, pet_id):
        """
        Deletes a pet
        :return:
        """
        url = urljoin(self.index_url, "/pets?id={0}".format(pet_id))

        return self.session.request('DELETE', url, self.headers)

    def place_order(self, order):
        """
        Places an order for a pet
        """
        url = urljoin(self.index_url, '/orders')

        return self.session.request('POST', url, self.headers, json.dumps(order))



