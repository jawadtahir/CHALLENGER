import multiprocessing
import os
import random
import urllib.request

from bs4 import BeautifulSoup
from tqdm import tqdm

# define the initial values
resource_url = "https://archive.sensor.community/csv_per_month/"
data_directory = 'luftdaten'
data_directory = '/home/chris/data/luftdaten'

def fetch_links(resource):
    urls = []
    try:
        resp = urllib.request.urlopen(resource)
        soup = BeautifulSoup(resp, "html5lib", from_encoding=resp.info().get_param('charset'))

        for link in soup.find_all('a', href=True):
            if link['href'] not in ['/', '../', 'temp/'] and "csv_per_month" not in link['href']:
                urls.append([resource + link['href'], link['href']])
    except Exception as e:
        print("Error occurred in fetching the data from: {}. Details:\n  {}".format(resource_url, e))

    return urls


def urls_every_month():
    download_urls = []
    for url, _ in tqdm(fetch_links(resource_url), desc="Fetching download URLs"):
        for fileurl, filename in fetch_links(url):
            local_filename = os.path.join(data_directory, filename)
            if not os.path.exists(local_filename):
                download_urls.append([fileurl, local_filename])
    return download_urls


def download_file(args):
    fileurl, local_filename = args
    if not os.path.exists(local_filename):
        temp_filename = os.path.join(os.path.dirname(local_filename), "partial_" + os.path.basename(local_filename))
        try:
            with urllib.request.urlopen(fileurl) as req:
                with open(temp_filename, 'wb') as f:
                    while True:
                        # Write 1MB chunks
                        chunk = req.read(1 * 1000 * 1000)
                        if not chunk:
                            break
                        f.write(chunk)

                os.rename(temp_filename, local_filename)
                return 0
        except:
            try:
                if os.path.exists(temp_filename):
                    os.remove(temp_filename)
            except:
                return 1
            return 1

        
def download_every_month():
    urls = urls_every_month()
    random.shuffle(urls)

    with multiprocessing.Pool(processes=5) as pool:
        failures = sum(tqdm(pool.imap_unordered(download_file, urls), total=len(urls), desc="Downloading files"))
        print("Total dowload failures: {}".format(failures))

        
def prepare_data_directory():
    if not os.path.exists(data_directory):
        os.makedirs(data_directory)


if __name__ == "__main__":
    prepare_data_directory()
    # fetch the url of the last directory
    download_every_month()
